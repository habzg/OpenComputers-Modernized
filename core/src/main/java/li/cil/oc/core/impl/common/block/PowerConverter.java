package li.cil.oc.core.impl.common.block;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.blockentity.traits.power.AE2Power;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PowerConverter extends SimpleBlock implements PowerAcceptor {
    public static BlockEntityType<?> TYPE;

    private static boolean isFabric;

    public static void setFabric(boolean value) {
        isFabric = value;
    }

    public PowerConverter(BlockEntityType<?> blockType) {
        super();
        TYPE = blockType;
    }

    public PowerConverter() {
        super();
    }

    @Override
    public ItemStack createItemStack(int amount) {
        if (OCSettings.get() != null && OCSettings.get().ignorePower) return ItemStack.EMPTY;
        return super.createItemStack(amount);
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().powerConverterRate;
    }

    @Override
    public void tooltipTail(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        super.tooltipTail(metadata, stack, player, tooltip, advanced);
        addRatio(tooltip, isFabric ? "rebornenergy" : "thermalexpansion", OCSettings.get().ratioRedstoneFlux());
        if (AE2Power.delegate() != null) {
            addRatio(tooltip, "appliedenergistics2", OCSettings.get().ratioAppliedEnergistics2());
        }
    }

    private void addRatio(List<Component> tooltip, String name, double ratio) {
        if (ratio <= 0) return;
        var a = ratio > 1 ? 1.0 : 1.0 / ratio;
        var b = ratio > 1 ? ratio : 1.0;
        tooltip.addAll(Tooltip.extended("powerconverter." + name, addExtension(a), addExtension(b)));
    }

    private static String addExtension(double x) {
        if (x >= 1e9) return NUMBER_FORMAT.format(x / 1e9) + "G";
        else if (x >= 1e6) return NUMBER_FORMAT.format(x / 1e6) + "M";
        else if (x >= 1e3) return NUMBER_FORMAT.format(x / 1e3) + "K";
        else return NUMBER_FORMAT.format(x);
    }

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.PowerConverter pc) {
            pc.onNeighborChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.PowerConverter(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == TYPE ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.blockentity.PowerConverter) te).updateEntity();
            } catch (Exception e) {
                Log.get().warn("Error in power converter tick", e);
            }
        } : null;
    }
}
