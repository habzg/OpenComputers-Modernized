package li.cil.oc.core.impl.common.blockentity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.common.blockentity.traits.Colored;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.common.blockentity.traits.TextBufferHost;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class Screen extends BlockEntity implements TextBufferHost, SidedEnvironment, Rotatable, RedstoneAware, Colored, Analyzable, Comparable<Screen> {

    public static BlockEntityType<Screen> TYPE;
    public final li.cil.oc.core.impl.common.component.TextBuffer buffer;
    public final Node node;
    public final Set<Screen> screens = new HashSet<>();
    public int tier;
    public boolean shouldCheckForMultiBlock = true;
    public int delayUntilCheckForMultiBlock = 40;
    public int width = 1, height = 1;
    public Screen origin = this;
    public boolean hadRedstoneInput = false;
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
    public AABB cachedBounds = null;
    public boolean invertTouchMode = false;
    public final Map<Entity, int[]> lastWalked = new WeakHashMap<>();
    public final Set<Entity> arrows = new HashSet<>();
    public int color;
    @SuppressWarnings("unused")
    private final boolean _isOutputEnabled = true;
    private Direction _pitch = Direction.NORTH;
    private Direction _yaw = Direction.SOUTH;

    public Screen(BlockPos pos, BlockState state) {
        this(pos, state, 0);
    }

    public Screen(BlockPos pos, BlockState state, int tier) {
        super(TYPE, pos, state);
        this.tier = tier;
        buffer = new li.cil.oc.core.impl.common.component.TextBuffer(this);
        buffer.setMaximumResolution(OCSettings.screenResolutionsByTier[tier][0], OCSettings.screenResolutionsByTier[tier][1]);
        buffer.setMaximumColorDepth(OCSettings.screenDepthsByTier[tier]);
        node = buffer.node();
        color = Color.byTier[tier];
        screens.add(this);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
            EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
        }
    }

    @Override
    public TextBuffer buffer() {
        return buffer;
    }

    @Override
    public int tier() {
        return tier;
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
        return node != null && node.address() != null && node.network() != null;
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
    public boolean canConnect(Direction side) {
        return toLocal(side) != Direction.SOUTH;
    }

    @Override
    public Node sidedNode(Direction side) {
        if (toLocal(side) != Direction.SOUTH) return node;
        var level = getLevel();
        if (level != null) {
            var neighborPos = worldPosition.relative(side);
            if (level.isLoaded(neighborPos)) {
                var neighbor = level.getBlockEntity(neighborPos);
                if (neighbor instanceof Keyboard) return node;
            }
        }
        return null;
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
    public Direction toLocal(Direction global) {
        return RotationHelper.toLocal(_pitch, _yaw, global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return RotationHelper.toGlobal(_pitch, _yaw, local);
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
    public void setFromFacing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            trySetPitchYaw(value, _yaw);
        } else {
            trySetPitchYaw(Direction.NORTH, value);
        }
    }

    @Override
    public void invertRotation() {
        Direction newPitch = (_pitch == Direction.DOWN || _pitch == Direction.UP) ? _pitch.getOpposite() : Direction.NORTH;
        Direction newYaw = _yaw.getOpposite();
        trySetPitchYaw(newPitch, newYaw);
    }

    @Override
    public Direction pitch() {
        return _pitch;
    }

    @Override
    public Direction yaw() {
        return _yaw;
    }

    public void trySetPitchYaw(Direction pitch, Direction yaw) {
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
            updateTranslation();
        }
    }

    private void updateTranslation() {
        if (getLevel() != null) {
            onRotationChanged();
        }
    }

    public boolean isOrigin() {
        return origin == this;
    }

    public int[] localPosition() {
        var lp = project(this);
        var op = project(origin);
        return new int[]{lp[0] - op[0], lp[1] - op[1]};
    }

    public boolean hasKeyboard() {
        var level = getLevel();
        if (level == null) return false;
        for (var screen : screens) {
            for (var side : Direction.values()) {
                var pos = new BlockPos(screen.worldPosition.getX() + side.getStepX(), screen.worldPosition.getY() + side.getStepY(), screen.worldPosition.getZ() + side.getStepZ());
                if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                    var te = level.getBlockEntity(pos);
                    if (te instanceof Keyboard keyboard && keyboard.hasNodeOnSide(side.getOpposite())) return true;
                }
            }
        }
        return false;
    }

    public void checkMultiBlock() {
        shouldCheckForMultiBlock = true;
        width = 1;
        height = 1;
        origin = this;
        screens.clear();
        screens.add(this);
        cachedBounds = null;
        invertTouchMode = false;
    }

    public double[] toScreenCoordinates(double hitX, double hitY, double hitZ) {
        double hx = toGlobal(Direction.EAST).getStepX() * hitX + toGlobal(Direction.EAST).getStepY() * hitY + toGlobal(Direction.EAST).getStepZ() * hitZ;
        double hy = toGlobal(Direction.UP).getStepX() * hitX + toGlobal(Direction.UP).getStepY() * hitY + toGlobal(Direction.UP).getStepZ() * hitZ;
        double tx = hx < 0 ? 1 + hx : hx;
        double ty = 1 - (hy < 0 ? 1 + hy : hy);
        var lp = localPosition();
        double ax = lp[0] + tx;
        double ay = height - 1 - lp[1] + ty;
        double border = 2.25 / 16.0;
        if (ax <= border || ay <= border || ax >= width - border || ay >= height - border) return new double[]{0.0};
        double iw = width - border * 2;
        double ih = height - border * 2;
        double rx = (ax - border) / iw;
        double ry = (ay - border) / ih;
        int bw = origin.buffer.getViewportWidth();
        int bh = origin.buffer.getViewportHeight();
        double bpw = origin.buffer.renderWidth() / iw;
        double bph = origin.buffer.renderHeight() / ih;
        double brx, bry;
        if (bpw > bph) {
            double rh = bph / bpw;
            bry = (ry - (1 - rh) * 0.5) / rh;
            brx = rx;
        } else if (bph > bpw) {
            double rw = bpw / bph;
            brx = (rx - (1 - rw) * 0.5) / rw;
            bry = ry;
        } else {
            brx = rx;
            bry = ry;
        }
        return new double[]{1.0, bry >= 0 && bry <= 1 && brx >= 0 && brx <= 1 ? 1.0 : 0.0, brx * bw, bry * bh};
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        buffer.update();
        RedstoneAware.super.updateEntity();
        if (shouldCheckForMultiBlock && ((isClient() && isClientReadyForMultiBlockCheck()) || (isServer() && isConnected()))) {
            var pending = new TreeSet<Screen>();
            var queue = new ArrayDeque<Screen>();
            pending.add(this);
            queue.add(this);
            while (!queue.isEmpty()) {
                var current = queue.poll();
                var p = project(current);
                // +X direction
                p[0] += 1;
                {
                    var np = unproject(p[0], p[1], p[2]);
                    var npPos = new BlockPos(np[0], np[1], np[2]);
                    var level = getLevel();
                    if (level != null && level.hasChunk(npPos.getX() >> 4, npPos.getZ() >> 4)) {
                        var te = level.getBlockEntity(npPos);
                        if (te instanceof Screen s && s.pitch() == pitch() && s.yaw() == yaw() && pending.add(s))
                            queue.add(s);
                    }
                }
                p[0] -= 1;
                // -X direction
                p[0] -= 1;
                {
                    var np = unproject(p[0], p[1], p[2]);
                    var npPos = new BlockPos(np[0], np[1], np[2]);
                    var level = getLevel();
                    if (level != null && level.hasChunk(npPos.getX() >> 4, npPos.getZ() >> 4)) {
                        var te = level.getBlockEntity(npPos);
                        if (te instanceof Screen s && s.pitch() == pitch() && s.yaw() == yaw() && pending.add(s))
                            queue.add(s);
                    }
                }
                p[0] += 1;
                // +Y direction
                p[1] += 1;
                {
                    var np = unproject(p[0], p[1], p[2]);
                    var npPos = new BlockPos(np[0], np[1], np[2]);
                    var level = getLevel();
                    if (level != null && level.hasChunk(npPos.getX() >> 4, npPos.getZ() >> 4)) {
                        var te = level.getBlockEntity(npPos);
                        if (te instanceof Screen s && s.pitch() == pitch() && s.yaw() == yaw() && pending.add(s))
                            queue.add(s);
                    }
                }
                p[1] -= 1;
                // -Y direction
                p[1] -= 1;
                {
                    var np = unproject(p[0], p[1], p[2]);
                    var npPos = new BlockPos(np[0], np[1], np[2]);
                    var level = getLevel();
                    if (level != null && level.hasChunk(npPos.getX() >> 4, npPos.getZ() >> 4)) {
                        var te = level.getBlockEntity(npPos);
                        if (te instanceof Screen s && s.pitch() == pitch() && s.yaw() == yaw() && pending.add(s))
                            queue.add(s);
                    }
                }
                p[1] += 1;
            }

            while (!pending.isEmpty()) {
                var current = pending.first();
                pending.remove(current);
                //noinspection StatementWithEmptyBody
                while (current.tryMerge()) {
                }
                if (isClient()) {
                    var bounds = current.origin.getRenderBoundingBox();
                    var level = getLevel();
                    if (level != null) {
                        level.blockUpdated(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), getBlockState().getBlock());
                        level.blockUpdated(BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ), getBlockState().getBlock());
                    }
                }
                for (var screen : current.screens) {
                    screen.shouldCheckForMultiBlock = false;
                    pending.remove(screen);
                    queue.add(screen);
                }
            }
            for (var screen : queue) {
                if (screen.isOrigin()) {
                    if (isServer()) {
                        ((Component) screen.buffer.node()).setVisibility(Visibility.Network);
                        screen.buffer.setEnergyCostPerTick(OCSettings.get().screenCost * screen.width * screen.height);
                        screen.buffer.setAspectRatio(screen.width, screen.height);
                    }
                } else {
                    if (isServer()) {
                        ((Component) screen.buffer.node()).setVisibility(Visibility.None);
                        screen.buffer.setEnergyCostPerTick(OCSettings.get().screenCost);
                    }
                    screen.buffer.setAspectRatio(1, 1);
                    int w = screen.buffer.getWidth();
                    int h = screen.buffer.getHeight();
                    screen.buffer.setForegroundColor(0xFFFFFF, false);
                    screen.buffer.setBackgroundColor(0x000000, false);
                    screen.buffer.fill(0, 0, w, h, 0x20);
                }
            }
        }
        if (isClient() && !arrows.isEmpty()) {
            for (Entity e : arrows) {
                double hitX = e.getX() - getBlockPos().getX();
                double hitY = e.getY() - getBlockPos().getY();
                double hitZ = e.getZ() - getBlockPos().getZ();
                if (e instanceof AbstractArrow arrow && arrow.getOwner() instanceof Player player && player == net.minecraft.client.Minecraft.getInstance().player) {
                    click(hitX, hitY, hitZ);
                }
            }
            arrows.clear();
        }
    }

    private boolean isClientReadyForMultiBlockCheck() {
        if (delayUntilCheckForMultiBlock > 0) {
            delayUntilCheckForMultiBlock--;
            return false;
        }
        return true;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer() && node != null) node.remove();
        for (var s : new HashSet<>(screens)) s.checkMultiBlock();
    }

    @Override
    public void onColorChanged() {
        if (isServer()) {
            PacketSender.sendColorChange(this, color());
        }
        for (var s : new HashSet<>(screens)) s.checkMultiBlock();
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        tier = Math.clamp(nbt.getByte(OCSettings.namespace + "tier"), 0, 2);
        color = Color.byTier[tier];
        buffer.setMaximumColorDepth(OCSettings.screenDepthsByTier[tier]);
        buffer.setMaximumResolution(OCSettings.screenResolutionsByTier[tier][0], OCSettings.screenResolutionsByTier[tier][1]);
        if (nbt.contains(OCSettings.namespace + "renderColor")) {
            color = nbt.getInt(OCSettings.namespace + "renderColor");
        }
        super.readFromNBTForServer(nbt);
        var provider = getEffectiveProvider();
        if (provider != null) buffer.load(nbt, provider);
        hadRedstoneInput = nbt.getBoolean(OCSettings.namespace + "hadRedstoneInput");
        invertTouchMode = nbt.getBoolean(OCSettings.namespace + "invertTouchMode");
        if (nbt.contains(OCSettings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "pitch"));
        }
        if (nbt.contains(OCSettings.namespace + "yaw")) {
            _yaw = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "yaw"));
        }
        buffer.setEnergyCostPerTick(OCSettings.get().screenCost);
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        nbt.putByte(OCSettings.namespace + "tier", (byte) tier);
        nbt.putInt(OCSettings.namespace + "renderColor", color);
        super.writeToNBTForServer(nbt);
        if (getEffectiveProvider() != null) buffer.save(nbt, getEffectiveProvider());
        nbt.putBoolean(OCSettings.namespace + "hadRedstoneInput", hadRedstoneInput);
        nbt.putBoolean(OCSettings.namespace + "invertTouchMode", invertTouchMode);
        nbt.putInt(OCSettings.namespace + "pitch", _pitch.get3DDataValue());
        nbt.putInt(OCSettings.namespace + "yaw", _yaw.get3DDataValue());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (getLevel() != null) {
            buffer.load(nbt, getLevel().registryAccess());
        }
        color = nbt.getInt("renderColor");
        invertTouchMode = nbt.getBoolean("invertTouchMode");
        _pitch = Direction.from3DDataValue(nbt.getInt("pitch"));
        _yaw = Direction.from3DDataValue(nbt.getInt("yaw"));
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        if (getLevel() != null) buffer.save(nbt, getLevel().registryAccess());
        nbt.putInt("renderColor", color);
        nbt.putBoolean("invertTouchMode", invertTouchMode);
        nbt.putInt("pitch", _pitch.get3DDataValue());
        nbt.putInt("yaw", _yaw.get3DDataValue());
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{origin.node};
    }

    @Override
    public int maxInput() {
        int max = 0;
        for (int v : _input) max = Math.max(max, Math.max(v, 0));
        return max;
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

    protected void onRedstoneInputChanged(RedstoneChangedEventArgs ignoredArgs) {
        boolean hasRedstoneInput = screens.stream().mapToInt(Screen::maxInput).max().orElse(0) > 0;
        if (hasRedstoneInput != hadRedstoneInput) {
            hadRedstoneInput = hasRedstoneInput;
            if (hasRedstoneInput) origin.buffer.setPowerState(!origin.buffer.getPowerState());
        }
    }

    @Override
    public void onRotationChanged() {
        if (isServer()) {
            PacketSender.sendRotatableState(this, pitch(), yaw());
        }
        for (var s : new HashSet<>(screens)) s.checkMultiBlock();
    }

    @Override
    public int compareTo(Screen that) {
        if (worldPosition.getX() != that.worldPosition.getX())
            return Integer.compare(worldPosition.getX(), that.worldPosition.getX());
        if (worldPosition.getY() != that.worldPosition.getY())
            return Integer.compare(worldPosition.getY(), that.worldPosition.getY());
        return Integer.compare(worldPosition.getZ(), that.worldPosition.getZ());
    }

    private boolean tryMerge() {
        var op = project(origin);
        return tryMergeTowards(op, 0, height) || tryMergeTowards(op, 0, -1) || tryMergeTowards(op, width, 0) || tryMergeTowards(op, -1, 0);
    }

    private boolean tryMergeTowards(int[] op, int dx, int dy) {
        var np = unproject(op[0] + dx, op[1] + dy, op[2]);
        var pos = new BlockPos(np[0], np[1], np[2]);
        var level = getLevel();
        if (level != null && level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            var te = level.getBlockEntity(pos);
            if (te instanceof Screen s && s.tier == tier && s.pitch() == pitch() && s.color == color && s.yaw() == yaw() && !screens.contains(s)) {
                var sp = project(s.origin);
                boolean canMergeAlongX = sp[1] == op[1] && s.height == height && s.width + width <= OCSettings.get().maxScreenWidth;
                boolean canMergeAlongY = sp[0] == op[0] && s.width == width && s.height + height <= OCSettings.get().maxScreenHeight;
                if (canMergeAlongX || canMergeAlongY) {
                    Screen newOrigin;
                    if (canMergeAlongX) newOrigin = sp[0] < op[0] ? s.origin : origin;
                    else newOrigin = sp[1] < op[1] ? s.origin : origin;
                    int newWidth = canMergeAlongX ? width + s.width : width;
                    int newHeight = canMergeAlongX ? height : height + s.height;
                    var newScreens = new HashSet<>(screens);
                    newScreens.addAll(s.screens);
                    for (var screen : newScreens) {
                        screen.width = newWidth;
                        screen.height = newHeight;
                        screen.origin = newOrigin;
                        screen.screens.addAll(newScreens);
                        screen.cachedBounds = null;
                        screen.setChanged();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private int[] project(Screen t) {
        int ex = toGlobal(Direction.EAST).getStepX() * t.worldPosition.getX() + toGlobal(Direction.EAST).getStepY() * t.worldPosition.getY() + toGlobal(Direction.EAST).getStepZ() * t.worldPosition.getZ();
        int uy = toGlobal(Direction.UP).getStepX() * t.worldPosition.getX() + toGlobal(Direction.UP).getStepY() * t.worldPosition.getY() + toGlobal(Direction.UP).getStepZ() * t.worldPosition.getZ();
        int sz = toGlobal(Direction.SOUTH).getStepX() * t.worldPosition.getX() + toGlobal(Direction.SOUTH).getStepY() * t.worldPosition.getY() + toGlobal(Direction.SOUTH).getStepZ() * t.worldPosition.getZ();
        return new int[]{ex, uy, sz};
    }

    private int[] unproject(int x, int y, int z) {
        int ex = toLocal(Direction.EAST).getStepX() * x + toLocal(Direction.EAST).getStepY() * y + toLocal(Direction.EAST).getStepZ() * z;
        int uy = toLocal(Direction.UP).getStepX() * x + toLocal(Direction.UP).getStepY() * y + toLocal(Direction.UP).getStepZ() * z;
        int sz = toLocal(Direction.SOUTH).getStepX() * x + toLocal(Direction.SOUTH).getStepY() * y + toLocal(Direction.SOUTH).getStepZ() * z;
        return new int[]{ex, uy, sz};
    }

    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        if ((width == 1 && height == 1) || !isOrigin())
            return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.0);
        if (cachedBounds != null) return cachedBounds;
        var sz = unproject(width, height, 1);
        var ox = worldPosition.getX() + (sz[0] < 0 ? 1 : 0);
        var oy = worldPosition.getY() + (sz[1] < 0 ? 1 : 0);
        var oz = worldPosition.getZ() + (sz[2] < 0 ? 1 : 0);
        var btmp = new net.minecraft.world.phys.AABB(
                ox, oy, oz,
                ox + sz[0], oy + sz[1], oz + sz[2]);
        cachedBounds = new net.minecraft.world.phys.AABB(
                Math.min(btmp.minX, btmp.maxX), Math.min(btmp.minY, btmp.maxY), Math.min(btmp.minZ, btmp.maxZ),
                Math.max(btmp.minX, btmp.maxX), Math.max(btmp.minY, btmp.maxY), Math.max(btmp.minZ, btmp.maxZ)).inflate(0.1);
        return cachedBounds;
    }

    @SuppressWarnings("unused")
    public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    public void color(int value) {
        if (value != color) {
            color = value;
            onColorChanged();
        }
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public void setColor(int value) {
        color = value;
    }

    @Override
    public boolean consumesDye() {
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean copyToAnalyzer(double hitX, double hitY, double hitZ, Player player) {
        double[] coords = toScreenCoordinates(hitX, hitY, hitZ);
        if (coords.length > 1 && coords[0] == 1.0 && coords[1] == 1.0) {
            origin.buffer.copyToAnalyzer((int) coords[3], player);
            return true;
        }
        return coords.length > 0 && coords[0] == 1.0;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean click(double hitX, double hitY, double hitZ) {
        double[] coords = toScreenCoordinates(hitX, hitY, hitZ);
        if (coords.length > 1 && coords[0] == 1.0 && coords[1] == 1.0) {
            origin.buffer.mouseDown(coords[2], coords[3], 0, null);
            return true;
        }
        return coords.length > 0 && coords[0] == 1.0;
    }

    public void walk(Entity entity) {
        int[] lp = localPosition();
        int x = lp[0];
        int y = lp[1];
        int[] oldPos = lastWalked.get(entity);
        if (oldPos != null && oldPos[0] == x && oldPos[1] == y) return;
        lastWalked.put(entity, new int[]{x, y});
        if (entity instanceof Player player && OCSettings.get().inputUsername) {
            origin.node.sendToReachable("computer.signal", "walk", x + 1, height - y, player.getName());
        } else {
            origin.node.sendToReachable("computer.signal", "walk", x + 1, height - y);
        }
    }

    public void shot(Entity entity) {
        if (entity instanceof AbstractArrow) {
            arrows.add(entity);
        }
    }
}
