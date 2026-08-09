package li.cil.oc.fabric.common.block;

import java.util.List;
import li.cil.oc.core.impl.common.block.traits.CustomDrops;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class SimpleBlock extends AbstractBlock {
    public SimpleBlock() {
        super();
    }

    public SimpleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!world.isClientSide && this instanceof CustomDrops customDrops) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null && customDrops.getBlockClass().isInstance(te)) {
                customDrops.doCustomDrops(te, player, true);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName()));
    }

    @Override
    public void tooltipTail(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        if (this instanceof PowerAcceptor acceptor) {
            tooltip.addAll(Tooltip.extended("PowerAcceptor", (int) acceptor.energyThroughput()));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide && this instanceof CustomDrops customDrops) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null && customDrops.getBlockClass().isInstance(te)) {
                customDrops.doCustomInit(te, placer, stack);
            }
        }
    }
}
