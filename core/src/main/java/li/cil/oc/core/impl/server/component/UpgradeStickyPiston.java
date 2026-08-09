package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public abstract class UpgradeStickyPiston extends UpgradePiston {
    public UpgradeStickyPiston(EnvironmentHost host) {
        super(host);
    }

    @Override
    public boolean isSticky() {
        return true;
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to reach out to the side given (default front) and pull a block similar to a vanilla sticky piston.")
    public Object[] pull(Context context, Arguments args) {
        Direction side = pushDirection(args, 0);
        BlockPosition hostPos = pushOrigin(side);
        var blockPos = new BlockPos(hostPos.x(), hostPos.y(), hostPos.z());
        BlockPos frontPos = blockPos.relative(side);
        BlockState frontState = host.level().getBlockState(frontPos);
        if (!frontState.isAir()) {
            if (frontState.getPistonPushReaction() == PushReaction.DESTROY) {
                host.level().destroyBlock(frontPos, true);
            } else {
                return ResultWrapper.result(false, "path is obstructed");
            }
        }
        if (movePiston(host.level(), blockPos, side, false)) {
            host.level().playSeededSound(null, host.xPosition(), host.yPosition(), host.zPosition(),
                    SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f,
                    host.level().random.nextFloat() * 0.25f + 0.6f, host.level().random.nextLong());
            context.pause(0.05);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "move failed");
    }

    public static class Drone extends UpgradeStickyPiston {
        public Drone(li.cil.oc.api.internal.Drone drone) {
            super(drone);
        }

        @Override
        public Direction pushDirection(Arguments args, int index) {
            return li.cil.oc.core.impl.util.ExtendedArguments.optSideAny(args, index, Direction.SOUTH);
        }
    }

    public static class Tablet extends Rotatable {
        public final li.cil.oc.api.internal.Tablet tablet;

        public Tablet(li.cil.oc.api.internal.Tablet tablet) {
            super(tablet);
            this.tablet = tablet;
        }

        @Override
        public BlockPosition pushOrigin(Direction side) {
            if (side == Direction.DOWN && tablet.player().getEyeHeight() > 1)
                return super.pushOrigin(side).offset(Direction.DOWN);
            return super.pushOrigin(side);
        }
    }

    public static class Rotatable extends UpgradeStickyPiston {
        public final li.cil.oc.api.internal.Rotatable rotatable;

        public Rotatable(li.cil.oc.api.internal.Rotatable rotatable) {
            super((EnvironmentHost) rotatable);
            this.rotatable = rotatable;
        }

        @Override
        public Direction pushDirection(Arguments args, int index) {
            return rotatable.toGlobal(li.cil.oc.core.impl.util.ExtendedArguments.optSideAny(args, index, Direction.SOUTH));
        }
    }
}
