package li.cil.oc.core.impl.common.blockentity;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.traits.Environment;
import li.cil.oc.core.impl.common.blockentity.traits.OpenSides;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetSplitter extends BlockEntity implements Environment, OpenSides, RedstoneAware, li.cil.oc.api.network.SidedEnvironment, DeviceInfo {

    public static BlockEntityType<NetSplitter> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("net_splitter", Visibility.Network)
            .create();
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
            DeviceInfo.DeviceAttribute.Description, "Ethernet controller",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "NetSplits",
            DeviceInfo.DeviceAttribute.Version, "1.0",
            DeviceInfo.DeviceAttribute.Width, "6"
    );
    public boolean isInverted = false;
    private final boolean[] _openSides = {true, true, true, true, true, true};
    @SuppressWarnings("unused")
    protected final boolean _isOutputEnabled;
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};

    public NetSplitter(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        _isOutputEnabled = true;
    }

    @Override
    public Node node() {
        return node;
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
        setChanged();
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

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean[] openSides() {
        return _openSides;
    }

    @Override
    public void openSides(boolean[] value) {
        if (value.length == _openSides.length) {
            System.arraycopy(value, 0, _openSides, 0, _openSides.length);
        }
    }

    @Override
    public byte compressSides() {
        byte result = 0;
        for (int i = 0; i < 6; i++) {
            if (_openSides[i]) result |= (byte) (1 << i);
        }
        return result;
    }

    @Override
    public void uncompressSides(byte value) {
        for (int i = 0; i < 6; i++) {
            _openSides[i] = (value & (1 << i)) != 0;
        }
    }

    @Override
    public boolean isSideOpen(Direction side) {
        return isInverted != _openSides[side.ordinal()];
    }

    @Override
    public void setSideOpen(Direction side, boolean value) {
        var previous = isSideOpen(side);
        _openSides[side.ordinal()] = value;
        if (previous != isSideOpen(side)) {
            setChanged();
            if (isServer()) {
                node.remove();
                li.cil.oc.api.Network.joinOrCreateNetwork(this);
                PacketSender.sendNetSplitterState(this, isInverted, compressSides());
                level().playSeededSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        net.minecraft.sounds.SoundEvents.PISTON_EXTEND, net.minecraft.sounds.SoundSource.BLOCKS,
                        0.5f, level().random.nextFloat() * 0.25f + 0.5f, level().random.nextLong());
                level().updateNeighborsAt(worldPosition, block());
            } else {
                level().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public Node sidedNode(Direction side) {
        return isSideOpen(side) ? node : null;
    }

    @Override
    public boolean canConnect(Direction side) {
        return isSideOpen(side);
    }

    @Override
    public void initialize() {
        super.initialize();
        EventHandlerDelegate.get().scheduleServer(this);
        EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
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
            onRedstoneInputChanged(new RedstoneChangedEventArgs(side, oldInput, value, -1));
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
            onRedstoneInputChanged(new RedstoneChangedEventArgs(side, oldValue, newValue, -1));
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

    protected void onRedstoneInputChanged(RedstoneAware.RedstoneChangedEventArgs args) {
        RedstoneAware.super.onRedstoneInputChanged(args.side().ordinal(), args.oldValue(), args.newValue(), args.color());
        var oldIsInverted = isInverted;
        isInverted = args.newValue() > 0;
        if (isInverted != oldIsInverted) {
            if (isServer()) {
                node.remove();
                li.cil.oc.api.Network.joinOrCreateNetwork(this);
                PacketSender.sendNetSplitterState(this, isInverted, compressSides());
                level().playSeededSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        net.minecraft.sounds.SoundEvents.PISTON_CONTRACT, net.minecraft.sounds.SoundSource.BLOCKS,
                        0.5f, level().random.nextFloat() * 0.25f + 0.7f, level().random.nextLong());
            } else {
                level().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        isInverted = nbt.getBoolean(OCSettings.namespace + "isInverted");
        if (nbt.contains(OCSettings.namespace + "openSides")) {
            uncompressSides(nbt.getByte(OCSettings.namespace + "openSides"));
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        nbt.putBoolean(OCSettings.namespace + "isInverted", isInverted);
        nbt.putByte(OCSettings.namespace + "openSides", compressSides());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        isInverted = nbt.getBoolean(OCSettings.namespace + "isInverted");
        if (nbt.contains(OCSettings.namespace + "openSides")) {
            uncompressSides(nbt.getByte(OCSettings.namespace + "openSides"));
        }
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putBoolean(OCSettings.namespace + "isInverted", isInverted);
        nbt.putByte(OCSettings.namespace + "openSides", compressSides());
    }

    public Map<Integer, Boolean> currentStatus() {
        var openSides = new HashMap<Integer, Boolean>();
        for (var side : Direction.values()) {
            openSides.put(side.ordinal(), isSideOpen(side));
        }
        return openSides;
    }

    public boolean setSide(Direction side, boolean state) {
        var previous = isSideOpen(side);
        setSideOpen(side, isInverted != state);
        return previous != state;
    }

    @Callback(doc = "function(settings:table):table -- set open state (true/false) of all sides in an array; index by direction. Returns previous states")
    public Object[] setSides(Context context, Arguments args) {
        var settings = args.checkTable(0);
        var previous = currentStatus();
        for (var side : Direction.values()) {
            int ordinal = side.ordinal();
            boolean value = false;
            if (settings.containsKey(ordinal)) {
                var v = settings.get(ordinal);
                if (v instanceof Boolean b) value = b;
            }
            setSide(side, value);
        }
        return (Object[]) result(previous);
    }

    @Callback(direct = true, doc = "function():table -- Returns current open/close state of all sides in an array, indexed by direction.")
    public Object[] getSides(Context context, Arguments args) {
        return (Object[]) result(currentStatus());
    }

    public Object[] setSideHelper(Arguments args, boolean value) {
        int sideIndex = args.checkInteger(0);
        if (sideIndex < 0 || sideIndex > 5) {
            return (Object[]) result(null, "invalid direction");
        }
        var side = Direction.from3DDataValue(sideIndex);
        return (Object[]) result(setSide(side, value));
    }

    @Callback(doc = "function(side: number):boolean -- Open the side, returns true if it changed to open.")
    public Object[] open(Context context, Arguments args) {
        return setSideHelper(args, true);
    }

    @Callback(doc = "function(side: number):boolean -- Close the side, returns true if it changed to close.")
    public Object[] close(Context context, Arguments args) {
        return setSideHelper(args, false);
    }

}
