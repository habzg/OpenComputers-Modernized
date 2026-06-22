package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PowerConverter extends SimpleBlock implements PowerAcceptor {
    public PowerConverter() {
        super();
    }

    @Override
    public ItemStack createItemStack(int amount) {
        if (Settings.get() != null && Settings.get().ignorePower) return ItemStack.EMPTY;
        return super.createItemStack(amount);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().powerConverterRate;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.PowerConverter pc) {
            pc.onNeighborChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.PowerConverter(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        var tileType = li.cil.oc.neoforge.common.init.TileEntities.POWER_CONVERTER.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.tileentity.PowerConverter) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in power converter tick", e);
            }
        } : null;
    }
}
