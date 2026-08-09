package li.cil.oc.core.impl.common.blockentity;

import java.util.Map;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.impl.common.blockentity.traits.Hub;
import li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Microcontroller extends BlockEntity implements PowerAcceptor, Hub, Computer, li.cil.oc.api.internal.Microcontroller, DeviceInfo, WorldlyContainer {

    public static BlockEntityType<Microcontroller> TYPE;
    public final MicrocontrollerData info = new MicrocontrollerData();
    public final boolean[] outputSides = new boolean[6];
    public final Node snooperNode;
    public final Node[] componentNodes;
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Microcontroller",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Cubicle",
            DeviceInfo.DeviceAttribute.Capacity, String.valueOf(getSizeInventory())
    );
    private Direction _facing = Direction.SOUTH;
    private Machine _machine;
    private CompoundTag pendingMachineNbt;
    private li.cil.oc.api.network.ManagedEnvironment[] _components;
    private boolean _isRunning = false;
    private boolean _hasErrored = false;

    public Microcontroller(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        for (int i = 0; i < 6; i++) outputSides[i] = true;
        snooperNode = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent("microcontroller")
                .withConnector(OCSettings.get().bufferMicrocontroller)
                .create();
        componentNodes = new Node[6];
        for (int i = 0; i < 6; i++) {
            componentNodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                    .withComponent("microcontroller")
                    .create();
        }
    }

    @Override
    public Node node() {
        return null;
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
        return snooperNode != null && snooperNode.address() != null && snooperNode.network() != null;
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
        if ("network.message".equals(message.name()) && message.source().network() == snooperNode.network()) {
            for (Direction side : Direction.values()) {
                if (outputSides[side.ordinal()] && side != facing()) {
                    Node node = sidedNode(side);
                    if (node != null) {
                        node.sendToReachable(message.name(), message.data());
                    }
                }
            }
        }
    }

    public void onNeighborChanged() {
        ae2OnNeighborChanged();
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
            if (pendingMachineNbt != null) {
                Machine m = machine();
                if (m != null) {
                    m.load(pendingMachineNbt, getEffectiveProvider());
                    setRunning(m.isRunning());
                }
                pendingMachineNbt = null;
            }
            if (machine() != null && machine().node() != null) {
                if (machine().node().network() == null) {
                    li.cil.oc.api.Network.joinNewNetwork(machine().node());
                }
                machine().node().connect(snooperNode);
            }
        }
    }

    @Override
    public int tier() {
        return info.tier;
    }

    @Override
    public String runSound() {
        return null;
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public Direction facing() {
        var state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        return _facing;
    }

    @Override
    public void facing(Direction value) {
        _facing = value;
        var state = getBlockState();
        if (getLevel() != null && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            getLevel().setBlockAndUpdate(worldPosition, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, value));
        }
    }

    @Override
    public void onRotationChanged() {
        if (isServer()) {
            PacketSender.sendRotatableState(this, pitch(), yaw());
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
    public Hub.Plug createPlug(Direction side) {
        return null;
    }

    @Override
    public void enqueuePacket(Direction sourceSide, li.cil.oc.api.network.Packet packet) {
    }

    @Override
    public boolean tryEnqueuePacket(Direction sourceSide, li.cil.oc.api.network.Packet packet) {
        return false;
    }

    @Override
    public Direction[] validDirections() {
        return Direction.values();
    }

    @Override
    public boolean canConnect(Direction side) {
        return false;
    }

    @Override
    public Node sidedNode(Direction side) {
        return null;
    }

    protected boolean hasConnector(Direction side) {
        return side != facing();
    }

    protected Connector connector(Direction side) {
        return side != facing() ? (Connector) snooperNode : null;
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
        var c = connector(side);
        if (c != null) {
            if (c.tryChangeBuffer(amount)) return amount;
        }
        return 0;
    }

    @Override
    public double globalBuffer(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBuffer() : 0.0;
    }

    @Override
    public double globalBufferSize(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBufferSize() : 0.0;
    }

    @Override
    public double globalDemand(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBufferSize() - c.globalBuffer() : 0.0;
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().caseRate[Tier.One];
    }

    @Override
    public net.minecraft.core.Direction toLocal(net.minecraft.core.Direction global) {
        return RotationHelper.toLocal(pitch(), yaw(), global);
    }

    @Override
    public net.minecraft.core.Direction toGlobal(net.minecraft.core.Direction local) {
        return RotationHelper.toGlobal(pitch(), yaw(), local);
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        if (side != facing()) return new Node[]{componentNodes[side.ordinal()]};
        return new Node[]{machine().node()};
    }

    @Override
    public void checkRedstoneInputChanged() {
    }

    @Override
    public boolean isUseableByPlayer(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    public void markDirty() {
        setChanged();
    }

    @Override
    public void onMachineConnect(li.cil.oc.api.network.Node node) {
    }

    @Override
    public void onMachineDisconnect(li.cil.oc.api.network.Node node) {
    }

    @Override
    public java.lang.Iterable<li.cil.oc.api.network.ManagedEnvironment> installedComponents() {
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (machine() != null && machine().isRunning())
            return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public java.lang.Iterable<ItemStack> internalComponents() {
        return info.components;
    }

    @Override
    public int componentSlot(String address) {
        var comps = componentEnvironments();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] != null && comps[i].node() != null && address.equals(comps[i].node().address())) return i;
        }
        return -1;
    }

    @Callback(doc = "function():boolean -- Starts the microcontroller. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) {
        return (Object[]) result(!machine().isPaused() && machine().start());
    }

    @Callback(doc = "function():boolean -- Stops the microcontroller. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) {
        return (Object[]) result(machine().stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the microcontroller is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return (Object[]) result(machine().isRunning());
    }

    @Callback(direct = true, doc = "function():string -- Returns the reason the microcontroller crashed, if applicable.")
    public Object[] lastError(Context context, Arguments args) {
        return (Object[]) result(machine().lastError());
    }

    @Callback(direct = true, doc = "function(side:number):boolean -- Get whether network messages are sent via the specified side.")
    public Object[] isSideOpen(Context context, Arguments args) {
        var side = ExtendedArguments.checkSideExcept(args, 0, facing());
        return (Object[]) result(outputSides[side.ordinal()]);
    }

    @Callback(doc = "function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.")
    public Object[] setSideOpen(Context context, Arguments args) {
        var side = ExtendedArguments.checkSideExcept(args, 0, facing());
        boolean oldValue = outputSides[side.ordinal()];
        outputSides[side.ordinal()] = args.checkBoolean(1);
        return (Object[]) result(oldValue);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (level().getGameTime() % OCSettings.get().tickFrequency == 0) {
            for (var side : Direction.values()) {
                if (side != facing()) {
                    if (sidedNode(side) instanceof Connector connector) {
                        double demand = ((Connector) snooperNode).globalBufferSize() - ((Connector) snooperNode).globalBuffer();
                        double available = demand + connector.changeBuffer(-demand);
                        ((Connector) snooperNode).changeBuffer(available);
                    }
                }
            }
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
                    setChanged();
                    PacketSender.sendComputerState(this, isRunning(), hasErrored());
                }
                updateComponents();
            }
        }
    }

    @Override
    public void connectItemNode(Node node) {
        if (machine() != null && machine().node() != null && node != null) {
            li.cil.oc.api.Network.joinNewNetwork(machine().node());
            machine().node().connect(node);
        }
    }

    @Override
    public Node createNode(Plug plug) {
        return li.cil.oc.api.Network.newNode(plug, Visibility.Network).withConnector().create();
    }

    @Override
    public void onPlugConnect(Plug plug, Node node) {
        if (node == plug.node()) {
            li.cil.oc.api.Network.joinNewNetwork(machine().node());
            machine().node().connect(snooperNode);
            connectComponents();
        }
        if (plug.isPrimary()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
    }

    @Override
    public void onPlugDisconnect(Plug plug, Node node) {
        if (plug.isPrimary() && node != plug.node()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
        if (node == plug.node()) disconnectComponents();
    }

    @Override
    public void onPlugMessage(Plug ignoredPlug, Message ignoredMessage) {}

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        var provider = getEffectiveProvider();
        if (provider != null) info.load(nbt.getCompound(OCSettings.namespace + "info"), provider);
        var outputsBytes = nbt.getByteArray(OCSettings.namespace + "outputs");
        for (int i = 0; i < Math.min(outputsBytes.length, outputSides.length); i++)
            outputSides[i] = outputsBytes[i] != 0;
        var tagList = nbt.getList(OCSettings.namespace + "componentNodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(tagList.size(), componentNodes.length); i++)
            if (provider != null) componentNodes[i].load(tagList.getCompound(i), provider);
        if (provider != null)
            snooperNode.load(nbt.getCompound(OCSettings.namespace + "snooper"), provider);
        super.readFromNBTForServer(nbt);
        if (nbt.contains(OCSettings.namespace + "computer")) {
            pendingMachineNbt = nbt.getCompound(OCSettings.namespace + "computer").copy();
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        if (_machine != null) {
            var computerTag = new CompoundTag();
            _machine.save(computerTag, getEffectiveProvider());
            nbt.put(OCSettings.namespace + "computer", computerTag);
        }
        ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "info", t -> info.save(t, getEffectiveProvider()));
        byte[] outputBytes = new byte[outputSides.length];
        for (int i = 0; i < outputSides.length; i++) outputBytes[i] = (byte) (outputSides[i] ? 1 : 0);
        nbt.putByteArray(OCSettings.namespace + "outputs", outputBytes);
        var tagList = new ListTag();
        for (var n : componentNodes) {
            var tag = new CompoundTag();
            n.save(tag, getEffectiveProvider());
            tagList.add(tag);
        }
        nbt.put(OCSettings.namespace + "componentNodes", tagList);
        ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "snooper", t -> snooperNode.save(t, getEffectiveProvider()));
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        var level = getLevel();
        if (level != null) info.load(nbt.getCompound("info"), level.registryAccess());
        super.readFromNBTForClient(nbt);
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        var level = getLevel();
        if (level != null)
            ExtendedNBT.setNewCompoundTag(nbt, "info", t -> info.save(t, level.registryAccess()));
    }

    @Override
    public ItemStack[] items() {
        return info.components.toArray(new ItemStack[0]);
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        if (slot >= 0 && slot < info.components.size()) {
            info.components.set(slot, stack);
        }
    }

    @Override
    public int getContainerSize() {
        return info.components.size();
    }

    @Override
    public int getSizeInventory() {
        return info.components.size();
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
    public void dropSlot(int slot) {
    }

    @Override
    public void dropSlot(int slot, int count, Direction direction) {
    }

    @Override
    public void dropAllSlots() {
    }

    @Override
    public void clearContent() {
    }

    @Override
    public li.cil.oc.api.network.EnvironmentHost host() {
        return this;
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment[] _components() {
        return _components;
    }

    @Override
    public void _components(li.cil.oc.api.network.ManagedEnvironment[] value) {
        _components = value;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
    }

    @Override
    public java.util.ArrayList<li.cil.oc.api.network.ManagedEnvironment> updatingComponents() {
        return new java.util.ArrayList<>();
    }

    @Override
    public Object[] getInterfaces(int side) {
        return new Object[0];
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
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return false;
    }

    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[0];
    }

    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, Direction side) {
        return false;
    }

    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
        return false;
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
    public li.cil.oc.api.machine.Machine machine() {
        if (_machine == null && isServer() && getLevel() != null) {
            _machine = li.cil.oc.api.Machine.create(this);
            ((Connector) _machine.node()).setLocalBufferSize(0);
            _machine.setCostPerTick(OCSettings.get().microcontrollerCost);
        }
        return _machine;
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
                getLevel().sendBlockUpdated(worldPosition, current, current, 3);
                getLevel().getLightEngine().checkBlock(worldPosition);
            }
        }
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
    public boolean isComponentSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canInteract(String player) {
        if (player == null) return false;
        if (!OCSettings.get().canComputersBeOwned) return true;
        if (users().isEmpty()) return true;
        return users().contains(player);
    }

    @Override
    public boolean hasRedstoneCard() {
        return false;
    }

    @Override
    public java.util.Set<String> users() {
        return java.util.Collections.emptySet();
    }

    @Override
    public void setUsers(Iterable<String> list) {
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < getSizeInventory()) {
            return info.components.get(slot);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack changeEEPROM(ItemStack newEeprom) {
        for (int i = 0; i < info.components.size(); i++) {
            var compInfo = li.cil.oc.api.Items.get(info.components.get(i));
            if (compInfo != null && Constants.ItemName.EEPROM.equals(compInfo.name())) {
                var oldEeprom = info.components.get(i);
                setItem(i, newEeprom);
                return oldEeprom;
            }
        }
        int lastSlot = getSizeInventory() - 1;
        var existing = getItem(lastSlot);
        if (!existing.isEmpty()) {
            setItem(lastSlot, newEeprom);
            return existing;
        }
        setItem(lastSlot, newEeprom);
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        Machine m = machine();
        if (m != null) {
            EventHandlerDelegate.get().scheduleServer(m::stop);
        }
    }
}
