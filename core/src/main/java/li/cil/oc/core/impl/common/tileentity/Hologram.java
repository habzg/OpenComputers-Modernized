package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.RotationHelper;
import li.cil.oc.core.impl.util.SaveHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

public class Hologram extends li.cil.oc.core.impl.common.tileentity.traits.TileEntity implements Environment, SidedEnvironment, Analyzable, Rotatable, DeviceInfo {
    public static BlockEntityType<?> TYPE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Hologram.class);
    public static final int WIDTH = 48;
    public static final int HEIGHT = 32;
    public static final int[][] COLORS_BY_TIER = {{0x00FF00}, {0x0000FF, 0x00FF00, 0xFF0000}};
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("hologram")
            .withConnector()
            .create();
    public final int[] volume = new int[WIDTH * WIDTH * 2];
    public final BitSet dirty = new BitSet(WIDTH * WIDTH);
    public final int[] colors;
    public int tier;
    private final Map<String, String> deviceInfo;
    public double scale = 1.0;
    public double translationX, translationY, translationZ;
    public double litRatio = -1.0;
    public boolean needsRendering = false;
    public int dirtyFromX = Integer.MAX_VALUE, dirtyUntilX = -1;
    public int dirtyFromZ = Integer.MAX_VALUE, dirtyUntilZ = -1;
    public boolean hasPower = true;
    public float rotationAngle, rotationX, rotationY, rotationZ;
    public float rotationSpeed, rotationSpeedX, rotationSpeedY, rotationSpeedZ;

    public Hologram(BlockPos pos, BlockState state) {
        this(pos, state, 0);
    }

    public Hologram(BlockPos pos, BlockState state, int tier) {
        super(TYPE, pos, state);
        this.tier = tier;
        colors = new int[COLORS_BY_TIER[tier].length];
        System.arraycopy(COLORS_BY_TIER[tier], 0, colors, 0, colors.length);
        deviceInfo = createDeviceInfo();
    }

    private Map<String, String> createDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
                DeviceInfo.DeviceAttribute.Description, "Holographic projector",
                DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceInfo.DeviceAttribute.Product, "VirtualViewer H1-" + (tier + 1),
                DeviceInfo.DeviceAttribute.Capacity, String.valueOf(WIDTH * WIDTH * HEIGHT),
                DeviceInfo.DeviceAttribute.Width, String.valueOf(colors.length)
        );
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
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

    private Direction facingDirection = Direction.SOUTH;
    private Direction _pitch = Direction.NORTH;

    @Override
    public Direction facing() {
        return _pitch == Direction.DOWN || _pitch == Direction.UP ? _pitch : facingDirection;
    }

    @Override
    public void facing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            _pitch = value;
        } else {
            _pitch = Direction.NORTH;
            facingDirection = value;
        }
    }

    public Direction pitch() {
        return _pitch;
    }

    public void pitch(Direction value) {
        _pitch = value;
    }

    public Direction yaw() {
        return facingDirection;
    }

    public void yaw(Direction value) {
        facingDirection = value;
    }

    @Override
    public Direction toLocal(Direction global) {
        return RotationHelper.toLocal(_pitch, facingDirection, global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return RotationHelper.toGlobal(_pitch, facingDirection, local);
    }

    @Override
    public void setFromFacing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            _pitch = value;
        } else {
            _pitch = Direction.NORTH;
            facingDirection = value;
        }
    }

    @Override
    public void setFromEntityPitchAndYaw(net.minecraft.world.entity.Entity entity) {
        Direction[] pitch2Direction = {Direction.UP, Direction.NORTH, Direction.DOWN};
        Direction[] yaw2Direction = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction newPitch = pitch2Direction[(int) Math.round(entity.getXRot() / 90.0) + 1];
        Direction newYaw = yaw2Direction[Math.round(entity.getYRot() / 360 * 4) & 3];
        _pitch = newPitch;
        facingDirection = newYaw;
    }

    @Override
    public void invertRotation() {
        var newPitch = (_pitch == Direction.DOWN || _pitch == Direction.UP) ? _pitch.getOpposite() : Direction.NORTH;
        var newYaw = facingDirection.getOpposite();
        _pitch = newPitch;
        facingDirection = newYaw;
    }

    @Override
    public void onRotationChanged() {
        syncFacingToBlockState();
    }

    public void syncFromBlockState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facingDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            var bf = state.getValue(BlockStateProperties.FACING);
            if (bf.getAxis().isVertical()) {
                _pitch = bf;
            } else {
                facingDirection = bf;
            }
        }
    }

    private void syncFacingToBlockState() {
        var level = getLevel();
        if (level == null) return;
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            var desired = _pitch.getAxis().isVertical() ? _pitch : facingDirection;
            if (desired.getAxis().isHorizontal()) {
                var current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (current != desired) {
                    level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.HORIZONTAL_FACING, desired), 3);
                }
            }
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            var desired = _pitch.getAxis().isVertical() ? _pitch : facingDirection;
            var current = state.getValue(BlockStateProperties.FACING);
            if (current != desired) {
                level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.FACING, desired), 3);
            }
        }
    }

    @Override
    public boolean canConnect(Direction side) {
        return toLocal(side) == Direction.DOWN;
    }

    @Override
    public Node sidedNode(Direction side) {
        return toLocal(side) == Direction.DOWN ? node : null;
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        return new Node[]{node};
    }

    public int getColor(int x, int y, int z) {
        int lbit = (volume[x + z * WIDTH] >>> y) & 1;
        int hbit = (volume[x + z * WIDTH + WIDTH * WIDTH] >>> y) & 1;
        return lbit | (hbit << 1);
    }

    public void setColor(int x, int y, int z, int value) {
        if ((value & 3) != getColor(x, y, z)) {
            int lbit = value & 1;
            int hbit = (value >>> 1) & 1;
            volume[x + z * WIDTH] = (volume[x + z * WIDTH] & ~(1 << y)) | (lbit << y);
            volume[x + z * WIDTH + WIDTH * WIDTH] = (volume[x + z * WIDTH + WIDTH * WIDTH] & ~(1 << y)) | (hbit << y);
            setDirty(x, z);
        }
    }

    private void setDirty(int x, int z) {
        dirty.set((x << 8) | z);
        dirtyFromX = Math.min(dirtyFromX, x);
        dirtyUntilX = Math.max(dirtyUntilX, x + 1);
        dirtyFromZ = Math.min(dirtyFromZ, z);
        dirtyUntilZ = Math.max(dirtyUntilZ, z + 1);
        litRatio = -1;
    }

    private void resetDirtyFlag() {
        dirty.clear();
        dirtyFromX = Integer.MAX_VALUE;
        dirtyUntilX = -1;
        dirtyFromZ = Integer.MAX_VALUE;
        dirtyUntilZ = -1;
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function() -- Clears the hologram.")
    public synchronized Object[] clear(Context context, Arguments args) {
        Arrays.fill(volume, 0);
        PacketSender.sendHologramClear(this);
        resetDirtyFlag();
        litRatio = 0;
        return null;
    }

    @Callback(direct = true, doc = "function(x:number, y:number, z:number):number -- Returns the value for the specified voxel.")
    public synchronized Object[] get(Context context, Arguments args) {
        var coords = checkCoordinates(args);
        return (Object[]) result(getColor(coords[0], coords[1], coords[2]));
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(direct = true, limit = 256, doc = "function(x:number, y:number, z:number, value:number or boolean) -- Set the value for the specified voxel.")
    public synchronized Object[] set(Context context, Arguments args) {
        var coords = checkCoordinates(args);
        int value = checkColor(args, 3);
        setColor(coords[0], coords[1], coords[2], value);
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(direct = true, limit = 128, doc = "function(x:number, z:number[, minY:number], maxY:number, value:number or boolean) -- Fills an interval of a column.")
    public synchronized Object[] fill(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        if (x < 0 || x >= WIDTH) throw new ArrayIndexOutOfBoundsException("x");
        int z = args.checkInteger(1) - 1;
        if (z < 0 || z >= WIDTH) throw new ArrayIndexOutOfBoundsException("z");
        int minY, maxY, value;
        if (args.count() > 4) {
            minY = Math.clamp(args.checkInteger(2), 1, 32);
            maxY = Math.clamp(args.checkInteger(3), 1, 32);
            value = checkColor(args, 4);
        } else {
            minY = 1;
            maxY = Math.clamp(args.checkInteger(2), 1, 32);
            value = checkColor(args, 3);
        }
        if (minY > maxY) throw new IllegalArgumentException("interval is empty");
        int mask = (int) (0xFFFFFFFFL >>> (31 - (maxY - minY))) << (minY - 1);
        int lbit = value & 1;
        int hbit = (value >>> 1) & 1;
        if (lbit == 0 || HEIGHT == 0) volume[x + z * WIDTH] &= ~mask;
        else volume[x + z * WIDTH] |= mask;
        if (hbit == 0 || HEIGHT == 0) volume[x + z * WIDTH + WIDTH * WIDTH] &= ~mask;
        else volume[x + z * WIDTH + WIDTH * WIDTH] |= mask;
        setDirty(x, z);
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(x:number, z:number, w:number, h:number, tx:number, tz:number) -- Copies an area of columns by the specified translation.")
    public synchronized Object[] copy(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        if (x < 0 || x >= WIDTH) throw new ArrayIndexOutOfBoundsException("x");
        int z = args.checkInteger(1) - 1;
        if (z < 0 || z >= WIDTH) throw new ArrayIndexOutOfBoundsException("z");
        int w = args.checkInteger(2);
        int h = args.checkInteger(3);
        int tx = args.checkInteger(4);
        int tz = args.checkInteger(5);
        if (w <= 0 || h <= 0) return null;
        if (tx == 0 && tz == 0) return null;
        int a = Math.clamp(x + tx + w - 1, 0, WIDTH - 1);
        int b = Math.clamp(x + tx, 0, WIDTH);
        int dx0 = tx > 0 ? a : b;
        int dx1 = tx > 0 ? b : a;
        int c = Math.clamp(z + tz + h - 1, 0, WIDTH - 1);
        int d = Math.clamp(z + tz, 0, WIDTH);
        int dz0 = tz > 0 ? c : d;
        int dz1 = tz > 0 ? d : c;
        int sx = tx > 0 ? -1 : 1;
        int sz = tz > 0 ? -1 : 1;
        for (int nz = dz0; (sz > 0 ? nz <= dz1 : nz >= dz1); nz += sz) {
            int oz = nz - tz;
            if (oz >= 0 && oz < WIDTH) {
                for (int nx = dx0; (sx > 0 ? nx <= dx1 : nx >= dx1); nx += sx) {
                    int ox = nx - tx;
                    if (ox >= 0 && ox < WIDTH) {
                        volume[nx + nz * WIDTH] = volume[ox + oz * WIDTH];
                        volume[nx + nz * WIDTH + WIDTH * WIDTH] = volume[ox + oz * WIDTH + WIDTH * WIDTH];
                        setDirty(nx, nz);
                    }
                }
            }
        }
        int area = (Math.max(dx0, dx1) - Math.min(dx0, dx1)) * (Math.max(dz0, dz1) - Math.min(dz0, dz1));
        context.pause(Math.max(0, area / (float) (WIDTH * WIDTH) - 0.25f));
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(data:string) -- Set the raw buffer.")
    public synchronized Object[] setRaw(Context context, Arguments args) {
        var data = args.checkByteArray(0);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < WIDTH; z++) {
                int offset = z * HEIGHT + x * HEIGHT * WIDTH;
                if (data.length >= offset + HEIGHT) {
                    int lbit = 0, hbit = 0;
                    for (int y = HEIGHT - 1; y >= 0; y--) {
                        int color = data[offset + y] & 0xFF;
                        lbit |= (color & 1) << y;
                        hbit |= ((color & 3) >>> 1) << y;
                    }
                    int index = x + z * WIDTH;
                    if (volume[index] != lbit || volume[index + WIDTH * WIDTH] != hbit) {
                        volume[index] = lbit;
                        volume[index + WIDTH * WIDTH] = hbit;
                        setDirty(x, z);
                    }
                }
            }
        }
        context.pause(Settings.get().hologramSetRawDelay);
        return null;
    }

    @Callback(direct = true, doc = "function():number -- Returns the render scale.")
    public Object[] getScale(Context context, Arguments args) {
        return (Object[]) result(scale);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(value:number) -- Set the render scale.")
    public Object[] setScale(Context context, Arguments args) {
        scale = Math.clamp(args.checkDouble(0), 0.333333, Settings.get().hologramMaxScaleByTier[tier]);
        PacketSender.sendHologramScale(this, scale);
        return null;
    }

    @Callback(direct = true, doc = "function():number, number, number -- Returns the translation offsets.")
    public Object[] getTranslation(Context context, Arguments args) {
        return (Object[]) result(translationX, translationY, translationZ);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(tx:number, ty:number, tz:number) -- Sets the translation offsets.")
    public Object[] setTranslation(Context context, Arguments args) {
        double maxTranslation = Settings.get().hologramMaxTranslationByTier[tier];
        translationX = Math.clamp(args.checkDouble(0), -maxTranslation, maxTranslation);
        translationY = Math.clamp(args.checkDouble(1), 0, maxTranslation * 2);
        translationZ = Math.clamp(args.checkDouble(2), -maxTranslation, maxTranslation);
        PacketSender.sendHologramOffset(this, translationX, translationY, translationZ);
        return null;
    }

    @Callback(direct = true, doc = "function():number -- The color depth supported.")
    public Object[] maxDepth(Context context, Arguments args) {
        return (Object[]) result(tier + 1);
    }

    @Callback(doc = "function(index:number):number -- Get the palette color.")
    public Object[] getPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 1 || index > colors.length) throw new ArrayIndexOutOfBoundsException();
        return (Object[]) result(convertColor(colors[index - 1]));
    }

    @Callback(doc = "function(index:number, value:number):number -- Set the palette color.")
    public Object[] setPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 1 || index > colors.length) throw new ArrayIndexOutOfBoundsException();
        int value = args.checkInteger(1);
        int oldValue = colors[index - 1];
        colors[index - 1] = convertColor(value);
        PacketSender.sendHologramColor(this, index - 1, colors[index - 1]);
        return (Object[]) result(oldValue);
    }

    @Callback(doc = "function(angle:number, x:number, y:number, z:number):boolean -- Set rotation.")
    public Object[] setRotation(Context context, Arguments args) {
        if (tier > 0) {
            rotationAngle = (float) (args.checkDouble(0) % 360);
            rotationX = (float) args.checkDouble(1);
            rotationY = (float) args.checkDouble(2);
            rotationZ = (float) args.checkDouble(3);
            PacketSender.sendHologramRotation(this, rotationAngle, rotationX, rotationY, rotationZ);
            return (Object[]) result(true);
        }
        return (Object[]) result(null, "not supported");
    }

    @Callback(doc = "function(speed:number, x:number, y:number, z:number):boolean -- Set rotation speed.")
    public Object[] setRotationSpeed(Context context, Arguments args) {
        if (tier > 0) {
            rotationSpeed = (float) Math.clamp(args.checkDouble(0), -360 * 4, 360 * 4);
            rotationSpeedX = (float) args.checkDouble(1);
            rotationSpeedY = (float) args.checkDouble(2);
            rotationSpeedZ = (float) args.checkDouble(3);
            PacketSender.sendHologramRotationSpeed(this, rotationSpeed, rotationSpeedX, rotationSpeedY, rotationSpeedZ);
            return (Object[]) result(true);
        }
        return (Object[]) result(null, "not supported");
    }

    @Callback(direct = true, doc = "function():number, number, number -- Get dimensions.")
    public Object[] getDimensions(Context context, Arguments args) {
        return (Object[]) result(WIDTH, HEIGHT, WIDTH);
    }

    private int[] checkCoordinates(Arguments args) {
        int x = args.checkInteger(0) - 1;
        if (x < 0 || x >= WIDTH) throw new ArrayIndexOutOfBoundsException("x");
        int y = args.checkInteger(1) - 1;
        if (y < 0 || y >= HEIGHT) throw new ArrayIndexOutOfBoundsException("y");
        int z = args.checkInteger(2) - 1;
        if (z < 0 || z >= WIDTH) throw new ArrayIndexOutOfBoundsException("z");
        return new int[]{x, y, z};
    }

    private int checkColor(Arguments args, int index) {
        int value;
        if (args.isBoolean(index)) value = args.checkBoolean(index) ? 1 : 0;
        else value = args.checkInteger(index);
        if (value < 0 || value > colors.length) throw new IllegalArgumentException("invalid value");
        return value;
    }

    private int convertColor(int color) {
        return ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16);
    }

    @Override
    public void initialize() {
        super.initialize();
        syncFromBlockState(getBlockState());
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) node.remove();
    }

    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            if (!dirty.isEmpty()) synchronized (this) {
                int dirtySizeX = dirtyUntilX - dirtyFromX;
                int dirtySizeZ = dirtyUntilZ - dirtyFromZ;
                if (dirty.cardinality() > dirtySizeX * dirtySizeZ * 0.8)
                    PacketSender.sendHologramArea(this, (byte) dirtyFromX, (byte) dirtyUntilX, (byte) dirtyFromZ, (byte) dirtyUntilZ, volume, WIDTH);
                else PacketSender.sendHologramValues(this, dirty, volume, WIDTH);
                resetDirtyFlag();
            }
            var level = getLevel();
            if (level != null && level.getGameTime() % Settings.get().tickFrequency == 0) {
                if (litRatio < 0) synchronized (this) {
                    litRatio = 0;
                    for (int v : volume) if (v != 0) litRatio += 1;
                    litRatio /= volume.length;
                }
                boolean hadPower = hasPower;
                double neededPower = Settings.get().hologramCost * litRatio * scale * Settings.get().tickFrequency;
                hasPower = ((li.cil.oc.api.network.Connector) node).tryChangeBuffer(-neededPower);
                if (hasPower != hadPower) PacketSender.sendHologramPowerChange(this, hasPower);
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        tier = Math.clamp(nbt.getByte(Settings.namespace + "tier"), 0, 1);
        if (nbt.contains(Settings.namespace + "yaw")) {
            facingDirection = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "yaw"));
        }
        if (nbt.contains(Settings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "pitch"));
        }
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facingDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            var bf = state.getValue(BlockStateProperties.FACING);
            if (bf.getAxis().isVertical()) {
                _pitch = bf;
            } else {
                facingDirection = bf;
            }
        }
        super.readFromNBTForServer(nbt);
        if (nbt.contains(Settings.namespace + "node")) {
            node.load(nbt.getCompound(Settings.namespace + "node"), getEffectiveProvider());
        }
        var tag = SaveHandlerDelegate.get().loadNBT(nbt, node.address() + "_data");
        var vol = tag.getIntArray("volume");
        System.arraycopy(vol, 0, volume, 0, Math.min(vol.length, volume.length));
        var cols = tag.getIntArray("colors");
        for (int i = 0; i < Math.min(cols.length, colors.length); i++) colors[i] = convertColor(cols[i]);
        scale = nbt.getDouble(Settings.namespace + "scale");
        translationX = nbt.getDouble(Settings.namespace + "offsetX");
        translationY = nbt.getDouble(Settings.namespace + "offsetY");
        translationZ = nbt.getDouble(Settings.namespace + "offsetZ");
        rotationAngle = nbt.getFloat(Settings.namespace + "rotationAngle");
        rotationX = nbt.getFloat(Settings.namespace + "rotationX");
        rotationY = nbt.getFloat(Settings.namespace + "rotationY");
        rotationZ = nbt.getFloat(Settings.namespace + "rotationZ");
        rotationSpeed = nbt.getFloat(Settings.namespace + "rotationSpeed");
        rotationSpeedX = nbt.getFloat(Settings.namespace + "rotationSpeedX");
        rotationSpeedY = nbt.getFloat(Settings.namespace + "rotationSpeedY");
        rotationSpeedZ = nbt.getFloat(Settings.namespace + "rotationSpeedZ");
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        nbt.putByte(Settings.namespace + "tier", (byte) tier);
        nbt.putInt(Settings.namespace + "yaw", facingDirection.get3DDataValue());
        nbt.putInt(Settings.namespace + "pitch", _pitch.get3DDataValue());
        super.writeToNBTForServer(nbt);
        var nodeTag = new CompoundTag();
        node.save(nodeTag, getEffectiveProvider());
        nbt.put(Settings.namespace + "node", nodeTag);
        try {
            var saveData = new net.minecraft.nbt.CompoundTag();
            saveData.putIntArray("volume", volume);
            var cols = new int[colors.length];
            for (int i = 0; i < colors.length; i++) cols[i] = convertColor(colors[i]);
            saveData.putIntArray("colors", cols);
            var baos = new java.io.ByteArrayOutputStream();
            var dos = new java.io.DataOutputStream(baos);
            net.minecraft.nbt.NbtIo.write(saveData, dos);
            SaveHandlerDelegate.get().scheduleSave(li.cil.oc.core.impl.util.BlockPosition.apply(this), nbt, node.address() + "_data", baos.toByteArray());
        } catch (java.io.IOException e) {
            LOGGER.warn("Error saving hologram data.", e);
        }
        nbt.putDouble(Settings.namespace + "scale", scale);
        nbt.putDouble(Settings.namespace + "offsetX", translationX);
        nbt.putDouble(Settings.namespace + "offsetY", translationY);
        nbt.putDouble(Settings.namespace + "offsetZ", translationZ);
        nbt.putFloat(Settings.namespace + "rotationAngle", rotationAngle);
        nbt.putFloat(Settings.namespace + "rotationX", rotationX);
        nbt.putFloat(Settings.namespace + "rotationY", rotationY);
        nbt.putFloat(Settings.namespace + "rotationZ", rotationZ);
        nbt.putFloat(Settings.namespace + "rotationSpeed", rotationSpeed);
        nbt.putFloat(Settings.namespace + "rotationSpeedX", rotationSpeedX);
        nbt.putFloat(Settings.namespace + "rotationSpeedY", rotationSpeedY);
        nbt.putFloat(Settings.namespace + "rotationSpeedZ", rotationSpeedZ);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (nbt.contains(Settings.namespace + "yaw")) {
            facingDirection = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "yaw"));
        }
        if (nbt.contains(Settings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(Settings.namespace + "pitch"));
        }
        var vol = nbt.getIntArray("volume");
        System.arraycopy(vol, 0, volume, 0, Math.min(vol.length, volume.length));
        var cols = nbt.getIntArray("colors");
        System.arraycopy(cols, 0, colors, 0, Math.min(cols.length, colors.length));
        scale = nbt.getDouble("scale");
        hasPower = nbt.getBoolean("hasPower");
        translationX = nbt.getDouble("offsetX");
        translationY = nbt.getDouble("offsetY");
        translationZ = nbt.getDouble("offsetZ");
        rotationAngle = nbt.getFloat("rotationAngle");
        rotationX = nbt.getFloat("rotationX");
        rotationY = nbt.getFloat("rotationY");
        rotationZ = nbt.getFloat("rotationZ");
        rotationSpeed = nbt.getFloat("rotationSpeed");
        rotationSpeedX = nbt.getFloat("rotationSpeedX");
        rotationSpeedY = nbt.getFloat("rotationSpeedY");
        rotationSpeedZ = nbt.getFloat("rotationSpeedZ");
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putInt(Settings.namespace + "yaw", facingDirection.get3DDataValue());
        nbt.putInt(Settings.namespace + "pitch", _pitch.get3DDataValue());
        nbt.putIntArray("volume", volume);
        nbt.putIntArray("colors", colors);
        nbt.putDouble("scale", scale);
        nbt.putBoolean("hasPower", hasPower);
        nbt.putDouble("offsetX", translationX);
        nbt.putDouble("offsetY", translationY);
        nbt.putDouble("offsetZ", translationZ);
        nbt.putFloat("rotationAngle", rotationAngle);
        nbt.putFloat("rotationX", rotationX);
        nbt.putFloat("rotationY", rotationY);
        nbt.putFloat("rotationZ", rotationZ);
        nbt.putFloat("rotationSpeed", rotationSpeed);
        nbt.putFloat("rotationSpeedX", rotationSpeedX);
        nbt.putFloat("rotationSpeedY", rotationSpeedY);
        nbt.putFloat("rotationSpeedZ", rotationSpeedZ);
    }
}
