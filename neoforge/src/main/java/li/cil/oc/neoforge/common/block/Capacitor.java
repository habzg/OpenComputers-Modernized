package li.cil.oc.neoforge.common.block;

import li.cil.oc.api.network.Connector;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Capacitor extends SimpleBlock {
    public Capacitor() {
        super(Properties.of()
                .randomTicks());
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Capacitor(pos, state);
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName(), (int) Settings.get().capacitorRate));
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Capacitor capacitor) {
            var connector = (Connector) capacitor.node();
            if (connector != null && connector.network() != null && connector.localBufferSize() > 0) {
                return Math.round(15f * (float) (connector.localBuffer() / connector.localBufferSize()));
            }
        }
        return 0;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Capacitor capacitor) {
            capacitor.recomputeCapacity(true);
        }
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull RandomSource random) {
        world.updateNeighbourForOutputSignal(pos, this);
    }
}
