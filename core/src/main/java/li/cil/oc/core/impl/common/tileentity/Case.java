package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.tileentity.traits.Colored;
import li.cil.oc.core.impl.common.tileentity.traits.Computer;
import li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.OCBlockStateProperties;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class Case extends TileEntity implements PowerAcceptor, Computer, Colored, li.cil.oc.api.internal.Case, DeviceInfo {

    public static BlockEntityType<Case> TYPE;
    public final Node node;
    public int tier;
    private Direction facing = Direction.SOUTH;
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Computer",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Blocker",
            DeviceInfo.DeviceAttribute.Capacity, String.valueOf(getContainerSize())
    );
    public long lastFileSystemAccess = 0;
    private boolean isSizeInventoryReady = true;
    private ManagedEnvironment[] _components;
    private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();
    private int _color;
    private Machine _machine;
    private boolean _isRunning = false;
    private boolean _hasErrored = false;
    private ItemStack[] _items = new ItemStack[0];
    private final java.util.Set<String> _users = new java.util.HashSet<>();
    private boolean _isOutputEnabled = false;
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
    private final int[] _output = new int[6];
    private final int[][] _bundledInput = new int[6][16];
    private final int[][] _bundledOutput = new int[6][16];
    private final int[][] _rednetInput = new int[6][16];

    public Case(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        this.tier = 0;
        node = null;
        _color = Color.byTier[tier];
    }

    public Case(BlockPos pos, BlockState state, int tier) {
        super(TYPE, pos, state);
        this.tier = tier;
        node = null;
        _color = Color.byTier[tier];
    }

    public Case(BlockPos pos, BlockState state, int tier, Node node) {
        super(TYPE, pos, state);
        this.tier = tier;
        this.node = node;
        _color = Color.byTier[tier];
    }

    @Override
    public Node node() {
        if (isServer()) {
            Machine m = machine();
            if (m != null) return m.node();
        }
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
    public void markDirty() {
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (isServer()) {
            Machine m = machine();
            if (m != null) {
                m.onHostChanged();
            }
            setOutputEnabled(hasRedstoneCard());
        }
    }

    private boolean isChangeScheduled = false;
    private CompoundTag pendingMachineNbt;

    public void markChanged() {
        isChangeScheduled = true;
    }

    public boolean isConnected() {
        Node n = node();
        return n != null && n.address() != null && n.network() != null;
    }

    @Override
    public void onConnect(Node node) {
        if (node == node()) {
            connectComponents();
        }
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
    }

    public void onNeighborChanged() {
        ae2OnNeighborChanged();
    }

    @Override
    public void onMachineConnect(Node node) {
        onConnect(node);
    }

    @Override
    public void onMachineDisconnect(Node node) {
        onDisconnect(node);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            if (pendingMachineNbt != null) {
                Machine m = machine();
                if (m != null) {
                    m.load(pendingMachineNbt, getEffectiveProvider());
                    setRunning(m.isRunning());
                }
                pendingMachineNbt = null;
            }
            EventHandlerDelegate.get().scheduleServer(this);
            EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
        }
    }

    @Override
    public java.util.Set<String> users() {
        return _users;
    }

    @Override
    public void setUsers(Iterable<String> list) {
        _users.clear();
        for (var u : list) _users.add(u);
    }

    @Override
    public boolean hasRedstoneCard() {
        if (machine() != null && !machine().isRunning()) return false;
        for (int i = 0; i < getContainerSize(); i++) {
            var stack = getItem(i);
            if (!stack.isEmpty()) {
                var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
                if (driver != null && li.cil.oc.core.impl.util.ComponentDriverHelper.isRedstoneCard(driver)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canInteract(String player) {
        return true;
    }

    @Override
    public boolean isRunning() {
        return _isRunning;
    }

    @Override
    public void setRunning(boolean value) {
        if (value != _isRunning) {
            _isRunning = value;
            if (value) _hasErrored = false;
            if (getLevel() != null) {
                BlockState current = getBlockState();
                BlockState newState = current.setValue(OCBlockStateProperties.CASE_RUNNING, value);
                if (getLevel().isClientSide) {
                    getLevel().sendBlockUpdated(worldPosition, current, newState, 3);
                } else {
                    getLevel().setBlock(worldPosition, newState, 3);
                }
            }
        }
    }

    @Override
    public boolean hasErrored() {
        return _hasErrored;
    }

    @Override
    public void hasErrored(boolean value) {
        _hasErrored = value;
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
    public Direction facing() {
        return facing;
    }

    @Override
    public void facing(Direction value) {
        facing = value;
        var level = getLevel();
        if (level != null && !level.isClientSide) {
            var state = getBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                level.setBlock(worldPosition, state.setValue(BlockStateProperties.HORIZONTAL_FACING, value), 3);
            }
        }
    }

    @Override
    public Direction toLocal(Direction global) {
        return RotationHelper.toLocal(pitch(), facing(), global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return RotationHelper.toGlobal(pitch(), facing(), local);
    }

    @Override
    public void onRotationChanged() {
        if (isServer()) {
            var level = getLevel();
            if (level == null) return;
            PacketSender.sendRotatableState(this, pitch(), yaw());
            var state = getBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                var current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (current != facing) {
                    level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 3);
                }
            }
        } else {
            var level = getLevel();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        var level = getLevel();
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        checkRedstoneInputChanged();
    }

    @Override
    public int color() {
        return _color;
    }

    @Override
    public void color(int value) {
        if (value != _color) {
            _color = value;
            onColorChanged();
        }
    }

    @Override
    public int getColor() {
        return color();
    }

    @Override
    public void setColor(int value) {
        color(value);
    }

    @Override
    public boolean consumesDye() {
        return true;
    }

    @Override
    public void onColorChanged() {
        if (isServer()) {
            PacketSender.sendColorChange(this, color());
        }
    }

    @Override
    public Machine machine() {
        if (_machine == null && isServer() && getLevel() != null) {
            _machine = li.cil.oc.api.Machine.create(this);
            if (isCreative() && _machine.node() instanceof Connector c) {
                c.changeBuffer(Double.POSITIVE_INFINITY);
            }
        }
        return _machine;
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && isComponentSlot(i, stack)) {
                result.add(stack);
            }
        }
        return result;
    }

    @Override
    public Iterable<ManagedEnvironment> installedComponents() {
        List<ManagedEnvironment> result = new ArrayList<>();
        if (_components != null) {
            for (ManagedEnvironment env : _components) {
                if (env != null) {
                    result.add(env);
                }
            }
        }
        return result;
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        Machine m = machine();
        return new Node[]{m != null ? m.node() : node()};
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public ManagedEnvironment[] _components() {
        return _components;
    }

    @Override
    public void _components(ManagedEnvironment[] value) {
        _components = value;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return isSizeInventoryReady;
    }

    @Override
    public ItemStack[] pendingRemovals() {
        return null;
    }

    @Override
    public ItemStack[] pendingAdds() {
        return null;
    }

    @Override
    public ArrayList<ManagedEnvironment> updatingComponents() {
        return updatingComponents;
    }

    @Override
    public EnvironmentHost host() {
        return this;
    }

    @Override
    public int x() {
        return worldPosition.getX();
    }

    @Override
    public int y() {
        return worldPosition.getY();
    }

    @Override
    public int z() {
        return worldPosition.getZ();
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return hasConnector(side);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        if (node() instanceof Connector c) {
            if (c.tryChangeBuffer(amount)) return amount;
            else return 0;
        }
        return 0;
    }

    @Override
    public double globalBuffer(Direction side) {
        if (node() instanceof Connector c) {
            return c.globalBuffer();
        }
        return 0;
    }

    @Override
    public double globalBufferSize(Direction side) {
        if (node() instanceof Connector c) {
            return c.globalBufferSize();
        }
        return 0;
    }

    @Override
    public double globalDemand(Direction side) {
        if (node() instanceof Connector c) {
            return Math.clamp(c.globalBufferSize() - c.globalBuffer(), 0, energyThroughput());
        }
        return 0;
    }

    protected boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public double energyThroughput() {
        return Settings.get().caseRate[tier];
    }

    @Override
    public boolean isOutputEnabled() {
        return _isOutputEnabled;
    }

    @Override
    public void setOutputEnabled(boolean value) {
        if (value != _isOutputEnabled) {
            _isOutputEnabled = value;
            onRedstoneOutputEnabledChanged();
        }
    }

    @Override
    public int[] input() {
        return _input;
    }

    @Override
    public int getInput(Direction side) {
        return Math.max(_input[side.ordinal()], 0);
    }

    @Override
    public void setInput(Direction side, int value) {
        var ord = side.ordinal();
        var old = _input[ord];
        _input[ord] = value;
        if (old >= 0 && old != value) {
            onRedstoneInputChanged(ord, old, value);
        }
    }

    @Override
    public void setInput(int[] values) {
        for (var side : Direction.values()) {
            int value = side.ordinal() < values.length ? values[side.ordinal()] : 0;
            setInput(side, value);
        }
    }

    @Override
    public int maxInput() {
        int max = 0;
        for (int v : _input) max = Math.max(max, Math.max(v, 0));
        return max;
    }

    @Override
    public int[] output() {
        return _output;
    }

    @Override
    public int getOutput(Direction side) {
        return _output[toLocal(side).ordinal()];
    }

    @Override
    public void setOutput(Direction side, int value) {
        var ord = toLocal(side).ordinal();
        if (_output[ord] != value) {
            _output[ord] = value;
            onRedstoneOutputChanged(side);
        }
    }

    @Override
    public void setOutput(Map<?, ?> values) {
        for (var side : Direction.values()) {
            var ord = toLocal(side).ordinal();
            var key = Integer.valueOf(ord);
            if (values.containsKey(key)) {
                var raw = values.get(key);
                if (raw instanceof Number num) {
                    setOutput(side, num.intValue());
                }
            }
        }
    }

    @Override
    public int[][] bundledInput() {
        return _bundledInput;
    }

    @Override
    public int[][] rednetInput() {
        return _rednetInput;
    }

    @Override
    public int[][] bundledOutput() {
        return _bundledOutput;
    }

    @Override
    public int getBundledInput(Direction side, int color) {
        return Math.max(_bundledInput[side.ordinal()][color], _rednetInput[side.ordinal()][color]);
    }

    @Override
    public void setBundledInput(Direction side, int color, int value) {
        _bundledInput[side.ordinal()][color] = Math.max(value, 0);
    }

    @Override
    public void setBundledInput(Direction side, int[] values) {
        var ord = side.ordinal();
        for (int i = 0; i < 16; i++) {
            _bundledInput[ord][i] = (values != null && i < values.length) ? Math.max(values[i], 0) : 0;
        }
    }

    @Override
    public int[] getBundledOutput(Direction side) {
        return _bundledOutput[toLocal(side).ordinal()];
    }

    @Override
    public int getBundledOutput(Direction side, int color) {
        return _bundledOutput[toLocal(side).ordinal()][color];
    }

    @Override
    public void setBundledOutput(Direction side, int color, int value) {
        var ord = toLocal(side).ordinal();
        var old = _bundledOutput[ord][color];
        if (old != value) {
            _bundledOutput[ord][color] = value;
            onRedstoneOutputChanged(side);
        }
    }

    @Override
    public void setBundledOutput(Direction side, Map<?, ?> values) {
        var ord = toLocal(side).ordinal();
        for (int i = 0; i < 16; i++) _bundledOutput[ord][i] = 0;
        for (var entry : values.entrySet()) {
            if (entry.getKey() instanceof Number key && entry.getValue() instanceof Number val) {
                var color = key.intValue();
                if (color >= 0 && color < 16) _bundledOutput[ord][color] = val.intValue();
            }
        }
    }

    @Override
    public void setBundledOutput(Map<?, ?> values) {
        for (var entry : values.entrySet()) {
            if (entry.getKey() instanceof Number sideKey && entry.getValue() instanceof Map<?, ?> sideMap) {
                var side = Direction.from3DDataValue(sideKey.intValue());
                setBundledOutput(side, sideMap);
            }
        }
    }

    @Override
    public void updateRedstoneInput(Direction side) {
        var oldValue = _input[side.ordinal()];
        var newValue = li.cil.oc.core.impl.integration.util.BundledRedstone.computeInput(position(), side);
        if (oldValue != newValue) {
            _input[side.ordinal()] = newValue;
            onRedstoneInputChanged(side.ordinal(), oldValue, newValue);
        }
        setBundledInput(side, li.cil.oc.core.impl.integration.util.BundledRedstone.computeBundledInput(position(), side));
    }

    @Override
    public void onRedstoneOutputChanged(Direction side) {
        if (getLevel() != null && !getLevel().isClientSide()) {
            getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
        syncRedstoneState();
    }

    @Override
    public Object[] getInterfaces(int side) {
        return new Object[0];
    }

    public boolean isCreative() {
        return tier == Tier.Four;
    }

    @Override
    public int componentSlot(String address) {
        var comps = _components();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] != null && comps[i].node() != null && address.equals(comps[i].node().address())) return i;
        }
        return -1;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isChangeScheduled) {
            setChanged();
            isChangeScheduled = false;
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
        if (isServer() && isCreative() && level().getGameTime() % (long) Settings.get().tickFrequency == 0) {
            if (node() instanceof Connector c) c.changeBuffer(Double.POSITIVE_INFINITY);
        }
        if (isServer() && isConnected()) {
            Machine m = machine();
            if (m != null) {
                m.update();
                boolean running = m.isRunning();
                String lastError = m.lastError();
                boolean errored = lastError != null;
                if (_isRunning != running || _hasErrored != errored) {
                    _isRunning = running;
                    _hasErrored = errored;
                    updateBlockState();
                    onRunningChanged();
                }
            }
            updateComponents();
        }
    }

    private void updateBlockState() {
        if (level() != null && !level().isClientSide) {
            BlockState current = level().getBlockState(worldPosition);
            BlockState newState = current.setValue(OCBlockStateProperties.CASE_RUNNING, _isRunning);
            if (current != newState) {
                level().setBlock(worldPosition, newState, 3);
            }
        }
    }

    protected void onRunningChanged() {
        setChanged();
        PacketSender.sendComputerState(this, isRunning(), hasErrored());
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        tier = Math.clamp(nbt.getByte(Settings.namespace + "tier"), 0, 3);
        _color = Color.byTier[tier];
        if (nbt.contains(Settings.namespace + "renderColor")) {
            _color = nbt.getInt(Settings.namespace + "renderColor");
        }
        if (nbt.contains(Settings.namespace + "facing")) {
            facing = Direction.from3DDataValue(nbt.getByte(Settings.namespace + "facing"));
        }
        super.readFromNBTForServer(nbt);
        isSizeInventoryReady = true;
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        nbt.putByte(Settings.namespace + "tier", (byte) tier);
        nbt.putInt(Settings.namespace + "renderColor", _color);
        nbt.putByte(Settings.namespace + "facing", (byte) facing.get3DDataValue());
        super.writeToNBTForServer(nbt);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        tier = nbt.getByte(Settings.namespace + "tier");
        _color = nbt.getInt("renderColor");
        _isRunning = nbt.getBoolean("isRunning");
        _hasErrored = nbt.getBoolean("hasErrored");
        if (nbt.contains(Settings.namespace + "facing")) {
            facing = Direction.from3DDataValue(nbt.getByte(Settings.namespace + "facing"));
        }
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        nbt.putByte(Settings.namespace + "tier", (byte) tier);
        nbt.putInt("renderColor", _color);
        nbt.putBoolean("isRunning", _isRunning);
        nbt.putBoolean("hasErrored", _hasErrored);
        nbt.putByte(Settings.namespace + "facing", (byte) facing.get3DDataValue());
        var provider = getEffectiveProvider();
        if (provider != null) {
            save(nbt, provider);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
        if (isServer()) {
            load(nbt, provider);
            if (nbt.contains(Settings.namespace + "computer")) {
                pendingMachineNbt = nbt.getCompound(Settings.namespace + "computer").copy();
            }
        } else {
            readFromNBTForClient(nbt);
            load(nbt, provider);
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
        if (isServer()) {
            Machine m = machine();
            if (m != null) {
                var computerTag = new CompoundTag();
                m.save(computerTag, provider);
                nbt.put(Settings.namespace + "computer", computerTag);
            }
        }
        save(nbt, provider);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket packet, HolderLookup.@NotNull Provider provider) {
        var tag = packet.getTag();
        readFromNBTForClient(tag);
        load(tag, provider);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider provider) {
        return super.getUpdateTag(provider);
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.handleUpdateTag(tag, provider);
        load(tag, provider);
        tier = tag.getByte(Settings.namespace + "tier");
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        Computer.super.onItemAdded(slot, stack);
        if (isServer() && InventorySlots.computer[tier][slot].slot().equals(Slot.Floppy)) {
            li.cil.oc.core.impl.common.Sound.playDiskInsert(this);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        Computer.super.onItemRemoved(slot, stack);
        if (isServer()) {
            var slotType = InventorySlots.computer[tier][slot].slot();
            if (slotType.equals(Slot.Floppy)) li.cil.oc.core.impl.common.Sound.playDiskEject(this);
            if (slotType.equals(Slot.CPU) && machine() != null) machine().stop();
        }
    }

    @Override
    public boolean isComponentSlot(int slot, ItemStack stack) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack[] items() {
        if (_items.length != getContainerSize()) {
            _items = new ItemStack[getContainerSize()];
        }
        return _items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        items()[slot] = stack;
    }

    private int effectiveTier() {
        return tier;
    }

    @Override
    public int getContainerSize() {
        int t = effectiveTier();
        if (t < 0 || t >= InventorySlots.computer.length) return 0;
        return InventorySlots.computer[t].length;
    }

    public int getSizeInventory() {
        return getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return getItem(slot);
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        return (!isCreative() || player.getAbilities().instabuild);
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        var driver = Driver.driverFor(stack, getClass());
        if (driver == null) return false;
        var provided = InventorySlots.computer[effectiveTier()][slot];
        return driver.slot(stack).equals(provided.slot()) && driver.tier(stack) <= provided.tier();
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
    public void spawnStackInWorld(ItemStack stack) {
        spawnStackInWorld(stack, null);
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction direction) {
        li.cil.oc.core.impl.util.InventoryUtils.spawnStackInWorld(
                li.cil.oc.core.impl.util.BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()),
                stack, direction, null);
    }

    @Override
    public void dropAllSlots() {
        for (int i = 0; i < getContainerSize(); i++) {
            dropSlot(i);
        }
    }

    @Override
    public void dropSlot(int slot) {
        dropSlot(slot, 1, null);
    }

    @Override
    public void dropSlot(int slot, int count, Direction direction) {
        var stack = getItem(slot);
        if (!stack.isEmpty()) {
            var toDrop = stack.split(count);
            if (direction != null) spawnStackInWorld(toDrop, direction);
            else spawnStackInWorld(toDrop);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        Machine m = machine();
        if (m != null) {
            m.stop();
        }
    }

}
