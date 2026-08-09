package li.cil.oc.core.impl.common.blockentity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.traits.Environment;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.RotationHelper;
import li.cil.oc.core.util.WaypointHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class Waypoint extends BlockEntity implements Environment, Rotatable, RedstoneAware, Nameable {
    public static BlockEntityType<Waypoint> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("waypoint")
            .create();
    public String label = "";
    private Direction facing = Direction.NORTH;
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};

    public Waypoint(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public @NotNull Component getName() {
        return hasCustomName() ? Component.literal(label) : Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public boolean hasCustomName() {
        return !label.isEmpty();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return hasCustomName() ? Component.literal(label) : null;
    }

    @Override
    public Level level() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double yPosition() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double zPosition() {
        return worldPosition.getZ() + 0.5;
    }

    @Override
    public void markChanged() {
    }

    @Override
    public boolean isConnected() {
        return node.address() != null && node.network() != null;
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    public Direction[] validFacings() {
        return Direction.values();
    }

    @Override
    public Direction facing() {
        return facing;
    }

    @Override
    public void facing(Direction value) {
        if (facing != value) {
            facing = value;
            onRotationChanged();
        }
    }

    @Override
    public Direction toLocal(Direction global) {
        return RotationHelper.toLocal(Direction.NORTH, facing, global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return RotationHelper.toGlobal(Direction.NORTH, facing, local);
    }

    @Override
    public void onRotationChanged() {
    }

    @Callback(doc = "function(): string -- Get the current label of this waypoint.")
    public Object[] getLabel(Context context, Arguments args) {
        return (Object[]) result(label);
    }

    @Callback(doc = "function(value:string) -- Set the label for this waypoint.")
    @SuppressWarnings("SameReturnValue")
    public Object[] setLabel(Context context, Arguments args) {
        label = args.checkString(0).substring(0, Math.min(32, args.checkString(0).length()));
        context.pause(0.5);
        return null;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        RedstoneAware.super.updateEntity();
        if (isClient()) {
            var dir = facing();
            var origin = position().toVec3().add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
            var dx = (level().random.nextFloat() - 0.5f) * 0.8f;
            var dy = (level().random.nextFloat() - 0.5f) * 0.8f;
            var dz = (level().random.nextFloat() - 0.5f) * 0.8f;
            var vx = (level().random.nextFloat() - 0.5f) * 0.2f + dir.getStepX() * 0.3f;
            var vy = (level().random.nextFloat() - 0.5f) * 0.2f + dir.getStepY() * 0.3f - 0.5f;
            var vz = (level().random.nextFloat() - 0.5f) * 0.2f + dir.getStepZ() * 0.3f;
            level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, origin.x + dx, origin.y + dy, origin.z + dz, vx, vy, vz);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
            EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
            WaypointHelper.get().add(this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        WaypointHelper.get().remove(this);
    }

    @Override
    public int[] input() {
        return _input;
    }

    @Override
    public void setInput(Direction side, int value) {
        int oldInput = _input[side.ordinal()];
        _input[side.ordinal()] = value;
        if (oldInput >= 0 && oldInput != value) {
            RedstoneAware.super.onRedstoneInputChanged(side.ordinal(), oldInput, value, -1);
        }
    }

    @Override
    public void setInput(int[] values) {
        for (int i = 0; i < values.length && i < _input.length; i++) {
            _input[i] = values[i];
        }
    }

    @Override
    public void updateRedstoneInput(Direction side) {
        int oldValue = _input[side.ordinal()];
        int newValue = li.cil.oc.core.impl.integration.util.BundledRedstone.computeInput(position(), side);
        if (oldValue != newValue) {
            _input[side.ordinal()] = newValue;
            RedstoneAware.super.onRedstoneInputChanged(side.ordinal(), oldValue, newValue, -1);
        }
    }

    @Override
    public void checkRedstoneInputChanged() {
        if (getLevel() != null && !getLevel().isClientSide) {
            for (Direction side : Direction.values()) {
                updateRedstoneInput(side);
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        label = nbt.getString(OCSettings.namespace + "label");
        if (nbt.contains(OCSettings.namespace + "yaw")) {
            facing = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "yaw"));
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        nbt.putString(OCSettings.namespace + "label", label);
        nbt.putInt(OCSettings.namespace + "yaw", facing.get3DDataValue());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        label = nbt.getString(OCSettings.namespace + "label");
        if (nbt.contains(OCSettings.namespace + "yaw")) {
            facing = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "yaw"));
        }
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putString(OCSettings.namespace + "label", label);
        nbt.putInt(OCSettings.namespace + "yaw", facing.get3DDataValue());
    }
}
