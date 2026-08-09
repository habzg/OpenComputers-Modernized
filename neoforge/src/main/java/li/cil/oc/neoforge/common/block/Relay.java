package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.neoforge.common.init.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Relay extends SimpleBlock implements PowerAcceptor, GUI {
    public Relay() {
        super();
    }

    @Override
    public int guiType() {
        return GuiType.Relay;
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().relayRate;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level ignoredLevel, @NotNull BlockState ignoredState, @NotNull BlockEntityType<T> type) {
        var relayType = BlockEntities.RELAY.get();
        return type == relayType ? (lvl, pos, st, te) -> ((li.cil.oc.core.impl.common.blockentity.Relay) te).updateEntity() : null;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Relay(pos, state);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Relay relay) {
            relay.onNeighborChanged();
        }
    }
}
