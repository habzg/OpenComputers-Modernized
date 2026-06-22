package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class UpgradePiston extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("piston")
            .withConnector()
            .create();
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Piston upgrade");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Displacer II+");
    }};

    public UpgradePiston(EnvironmentHost host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    public abstract Direction pushDirection(Arguments args, int index);

    public BlockPosition pushOrigin(Direction side) {
        return BlockPosition.apply(host);
    }

    public boolean isSticky() {
        return false;
    }

    @Callback(doc = "function():boolean -- Returns true if the piston is sticky, i.e. it can also pull.")
    public Object[] isSticky(Context context, Arguments args) {
        return ResultWrapper.result(isSticky());
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.")
    public Object[] push(Context context, Arguments args) {
        Direction side = pushDirection(args, 0);
        BlockPosition hostPos = pushOrigin(side);
        var blockPos = new BlockPos(hostPos.x(), hostPos.y(), hostPos.z());
        if (!((Connector) node).tryChangeBuffer(-Settings.get().pistonCost)) {
            return ResultWrapper.result(false, "not enough energy");
        }
        if (pushBlocks(host.level(), blockPos, side)) {
            host.level().playSeededSound(null, host.xPosition(), host.yPosition(), host.zPosition(),
                    SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f,
                    host.level().random.nextFloat() * 0.25f + 0.6f, host.level().random.nextLong());
            context.pause(0.5);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "move failed");
    }

    @Callback(doc = "function():boolean, string -- Sticky pistons only. Tries to pull the block on the specified side.")
    public Object[] pull(Context context, Arguments args) {
        return ResultWrapper.result(false, "piston is not sticky. does not have pull");
    }

    private static boolean pushBlocks(Level level, BlockPos startPos, Direction dir) {
        List<BlockPos> toPush = new ArrayList<>();
        BlockPos currentPos = startPos.relative(dir);
        for (int i = 0; i < 12; i++) {
            BlockState state = level.getBlockState(currentPos);
            if (state.isAir()) break;
            PushReaction reaction = state.getPistonPushReaction();
            if (reaction == PushReaction.BLOCK || reaction == PushReaction.IGNORE) return false;
            toPush.add(currentPos);
            currentPos = currentPos.relative(dir);
        }
        if (toPush.isEmpty()) return false;
        for (int i = toPush.size() - 1; i >= 0; i--) {
            BlockPos pushPos = toPush.get(i);
            BlockState pushState = level.getBlockState(pushPos);
            BlockPos targetPos = pushPos.relative(dir);
            level.setBlock(targetPos, pushState, 2);
            level.setBlock(pushPos, Blocks.AIR.defaultBlockState(), 2 | 1024);
        }
        return true;
    }

    public static class Drone extends UpgradePiston {
        public Drone(li.cil.oc.api.internal.Drone drone) {
            super(drone);
        }

        @Override
        public Direction pushDirection(Arguments args, int index) {
            return ExtendedArguments.optSideAny(args, index, Direction.SOUTH);
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

    public static class Rotatable extends UpgradePiston {
        public final li.cil.oc.api.internal.Rotatable rotatable;

        public Rotatable(li.cil.oc.api.internal.Rotatable rotatable) {
            super((EnvironmentHost) rotatable);
            this.rotatable = rotatable;
        }

        @Override
        public Direction pushDirection(Arguments args, int index) {
            return rotatable.toGlobal(ExtendedArguments.optSideAny(args, index, Direction.SOUTH));
        }
    }
}
