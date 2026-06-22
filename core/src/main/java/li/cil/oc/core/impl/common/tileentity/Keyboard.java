package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


public class Keyboard extends TileEntity implements Environment, Rotatable, SidedEnvironment, Analyzable {

    public static BlockEntityType<Keyboard> TYPE;
    private final ManagedEnvironment keyboard;
    private Direction _pitch = Direction.NORTH;
    private Direction _yaw = Direction.SOUTH;

    public Keyboard(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        var keyboardItem = li.cil.oc.api.Items.get(Constants.BlockName.Keyboard).createItemStack(1);
        var driver = li.cil.oc.api.API.driver.driverFor(keyboardItem, getClass());
        keyboard = (ManagedEnvironment) driver.createEnvironment(keyboardItem, this);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public Node node() {
        return keyboard.node();
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
        return node() != null && node().address() != null && node().network() != null;
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
        return _pitch == Direction.DOWN || _pitch == Direction.UP ? _pitch : _yaw;
    }

    @Override
    public void facing(Direction value) {
        setFromFacing(value);
    }

    @Override
    public void setFromFacing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            trySetPitchYaw(value, _yaw);
        } else {
            trySetPitchYaw(Direction.NORTH, value);
        }
    }

    @Override
    public void setFromEntityPitchAndYaw(net.minecraft.world.entity.Entity entity) {
        Direction[] pitch2Direction = {Direction.UP, Direction.NORTH, Direction.DOWN};
        Direction[] yaw2Direction = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction newPitch = pitch2Direction[(int) Math.round(entity.getXRot() / 90.0) + 1];
        Direction newYaw = yaw2Direction[Math.round(entity.getYRot() / 360 * 4) & 3];
        trySetPitchYaw(newPitch, newYaw);
    }

    @Override
    public void invertRotation() {
        Direction newPitch = (_pitch == Direction.DOWN || _pitch == Direction.UP) ? _pitch.getOpposite() : Direction.NORTH;
        Direction newYaw = _yaw.getOpposite();
        trySetPitchYaw(newPitch, newYaw);
    }

    @Override
    public Direction toLocal(Direction global) {
        return li.cil.oc.core.impl.util.RotationHelper.toLocal(_pitch, _yaw, global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return li.cil.oc.core.impl.util.RotationHelper.toGlobal(_pitch, _yaw, local);
    }

    @Override
    public void onRotationChanged() {
    }

    @Override
    public Direction pitch() {
        return _pitch;
    }

    @Override
    public Direction yaw() {
        return _yaw;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean trySetPitchYaw(Direction pitch, Direction yaw) {
        boolean changed = false;
        if (pitch != _pitch) {
            _pitch = pitch;
            changed = true;
        }
        if (yaw != _yaw) {
            _yaw = yaw;
            changed = true;
        }
        if (changed) {
            onRotationChanged();
        }
        return changed;
    }

    public boolean hasNodeOnSide(Direction side) {
        return side != facing() && (isOnWall() || side != forward().getOpposite());
    }

    @Override
    public boolean canConnect(Direction side) {
        return hasNodeOnSide(side);
    }

    @Override
    public Node sidedNode(Direction side) {
        return hasNodeOnSide(side) ? node() : null;
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        return new Node[]{node()};
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        var provider = getEffectiveProvider();
        if (isServer() && provider != null && nbt.contains(Settings.namespace + "keyboard")) {
            keyboard.load(nbt.getCompound(Settings.namespace + "keyboard"), provider);
        }
        if (nbt.contains(Settings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "pitch"));
        }
        if (nbt.contains(Settings.namespace + "yaw")) {
            _yaw = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "yaw"));
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        var provider = getEffectiveProvider();
        if (isServer() && provider != null) {
            ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "keyboard", t -> keyboard.save(t, provider));
        }
        nbt.putInt(Settings.namespace + "pitch", _pitch.get3DDataValue());
        nbt.putInt(Settings.namespace + "yaw", _yaw.get3DDataValue());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (nbt.contains(Settings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "pitch"));
        }
        if (nbt.contains(Settings.namespace + "yaw")) {
            _yaw = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "yaw"));
        }
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putInt(Settings.namespace + "pitch", _pitch.get3DDataValue());
        nbt.putInt(Settings.namespace + "yaw", _yaw.get3DDataValue());
    }

    private boolean isOnWall() {
        return _pitch != Direction.UP && _pitch != Direction.DOWN;
    }

    private Direction forward() {
        return isOnWall() ? Direction.UP : _yaw;
    }

}
