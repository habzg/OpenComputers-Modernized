package li.cil.oc.core.impl.server.component;

import java.util.List;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;

public abstract class UpgradePiston extends AbstractManagedEnvironment implements DeviceInfo {
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

    public BlockPosition pushOrigin(Direction ignoredSide) {
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
        if (movePiston(host.level(), blockPos, side, true)) {
            host.level().playSeededSound(null, host.xPosition(), host.yPosition(), host.zPosition(),
                    SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f,
                    host.level().random.nextFloat() * 0.25f + 0.6f, host.level().random.nextLong());
            context.pause(0.05);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "move failed");
    }

    static boolean movePiston(Level level, BlockPos pistonPos, Direction dir, boolean extending) {
        PistonStructureResolver resolver = new PistonStructureResolver(level, pistonPos, dir, extending);
        if (!resolver.resolve()) return false;
        List<BlockPos> toPush = resolver.getToPush();
        List<BlockPos> toDestroy = resolver.getToDestroy();
        Direction moveDir = resolver.getPushDirection();
        for (BlockPos pos : toDestroy) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                Block.dropResources(state, level, pos, be);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
        }
        for (int i = toPush.size() - 1; i >= 0; i--) {
            BlockPos pos = toPush.get(i);
            BlockState state = level.getBlockState(pos);
            BlockPos target = pos.relative(moveDir);
            level.setBlock(target, state, 2);
            level.removeBlock(pos, false);
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
