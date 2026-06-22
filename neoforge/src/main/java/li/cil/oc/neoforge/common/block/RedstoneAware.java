package li.cil.oc.neoforge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class RedstoneAware extends SimpleBlock {
    public RedstoneAware() {
        super();
    }

    public RedstoneAware(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(@NotNull BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState state, BlockGetter world, @NotNull BlockPos pos, Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.traits.RedstoneAware redstone) {
            return redstone.isOutputEnabled();
        }
        return false;
    }

    @Override
    public int getSignal(@NotNull BlockState state, BlockGetter world, @NotNull BlockPos pos, @NotNull Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.traits.RedstoneAware redstone) {
            return Math.max(0, redstone.getOutput(side.getOpposite()));
        }
        return 0;
    }

    @Override
    public int getDirectSignal(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull Direction side) {
        return getSignal(state, world, pos, side);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.traits.RedstoneAware redstone) {
            if (!world.isClientSide) redstone.checkRedstoneInputChanged();
        }
    }
}
