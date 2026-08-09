package li.cil.oc.core.impl.common.blockentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.impl.common.blockentity.traits.PowerInformation;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.RotationHelper;
import li.cil.oc.core.util.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

public abstract class Robot extends RobotBase implements Computer, PowerInformation, li.cil.oc.api.internal.Robot {
    public RobotProxy proxy;
    public final MultiTank tank;
    private Player player_;
    private boolean isConnecting = false;
    private ManagedEnvironment[] _comps;
    private final ArrayList<ManagedEnvironment> updatingComponentsList = new ArrayList<>();
    private CompoundTag pendingMachineNbt;

    public Robot(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        hostClass = getClass();
        node = machine().node();
        bot = new li.cil.oc.core.impl.server.component.Robot(this);
        tank = new MultiTank() {
            @Override
            public int tankCount() {
                return Robot.this.tankCount();
            }

            @Override
            public Object getFluidTank(int index) {
                return Robot.this.getFluidTank(index);
            }
        };
    }

    protected abstract Player createAgentPlayer(ServerLevel sl);

    public abstract void setBlockPos(BlockPos pos);

    @Override
    public int tier() {
        return info.tier;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setRunning(boolean value) {
        isRunning = value;
    }

    @Override
    public boolean hasErrored() {
        return hasErrored;
    }

    @Override
    public void hasErrored(boolean value) {
        hasErrored = value;
    }

    @Override
    public boolean canInteract(String player) {
        if (player == null) return false;
        if (!OCSettings.get().canComputersBeOwned) return true;
        if (users.isEmpty()) return true;
        return users.contains(player);
    }

    @Override
    public void setUsers(Iterable<String> list) {
        users.clear();
        list.forEach(users::add);
    }

    @Override
    public Set<String> users() {
        return users;
    }

    protected abstract boolean hasRedstoneCardInInventory();

    @Override
    public boolean hasRedstoneCard() {
        return hasRedstoneCardInInventory();
    }

    @Override
    public boolean isComponentSlot(int slot, ItemStack stack) {
        return isContainerSlot(slot) || componentSlots().contains(slot);
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return info.components;
    }

    @Override
    public Iterable<ManagedEnvironment> installedComponents() {
        List<ManagedEnvironment> result = new ArrayList<>();
        var comps = _components();
        if (comps != null) {
            for (ManagedEnvironment env : comps) {
                if (env != null) {
                    result.add(env);
                }
            }
        }
        return result;
    }

    @Override
    public int componentSlot(String address) {
        var comps = _components();
        if (comps != null) {
            for (int i = 0; i < comps.length; i++) {
                if (comps[i] != null && comps[i].node() != null && address.equals(comps[i].node().address()))
                    return i;
            }
        }
        return -1;
    }

    protected abstract void postAnalyzeEvent(li.cil.oc.api.network.Node[] ignoredNodes, Player player);

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.robotowner", ownerName));
        if (player() != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.robotname", player().getDisplayName()));
        }
        postAnalyzeEvent(new Node[]{machine().node()}, player);
        return new Node[]{machine().node()};
    }

    @Override
    public void onRotationChanged() {
        syncFacingToBlockState();
        setChanged();
        var level = getLevel();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public double energyThroughput() {
        return 0;
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return true;
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return machine.node() instanceof Connector c ? c.changeBuffer(amount) : amount;
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        return tryChangeBuffer(side, amount);
    }

    @Override
    public double globalBuffer(Direction side) {
        return globalBuffer;
    }

    @Override
    public double globalBufferSize(Direction side) {
        return globalBufferSize;
    }

    @Override
    public double globalDemand(Direction side) {
        return 0;
    }

    @Override
    public double globalBuffer() {
        return globalBuffer;
    }

    @Override
    public void globalBuffer(double value) {
        globalBuffer = value;
    }

    @Override
    public double globalBufferSize() {
        return globalBufferSize;
    }

    @Override
    public void globalBufferSize(double value) {
        globalBufferSize = value;
    }

    @Override
    public void updatePowerInformation() {
    }

    @Override
    public int componentCount() {
        return info.components.size();
    }

    @Override
    public ManagedEnvironment getComponentInSlot(int index) {
        var comps = _components();
        if (comps != null && index >= 0 && index < comps.length) return comps[index];
        return null;
    }

    @Override
    public void synchronizeSlot(int slot) {
        if (slot >= 0 && slot < getContainerSize()) {
            var stack = getItem(slot);
            var comps = _components();
            if (comps != null && slot < comps.length && comps[slot] != null) {
                var driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver != null) {
                    save(comps[slot], driver, stack, level().registryAccess());
                }
            }
            PacketSender.sendRobotInventory(this, slot, stack);
        }
    }

    @Override
    public boolean shouldAnimate() {
        return isRunning;
    }

    @Override
    public ManagedEnvironment[] _components() {
        return _comps;
    }

    @Override
    public void _components(ManagedEnvironment[] value) {
        _comps = value;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
    }

    @Override
    public ArrayList<ManagedEnvironment> updatingComponents() {
        return updatingComponentsList;
    }

    @Override
    public EnvironmentHost host() {
        return this;
    }

    @Override
    public Object[] getInterfaces(int side) {
        return new Object[0];
    }

    @Override
    public ItemStack[] items() {
        return inventory;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        ItemStack oldStack = getItem(slot);
        super.setItem(slot, stack);
        ItemStack newStack = getItem(slot);
        if (!ItemStack.matches(oldStack, newStack)) {
            if (!oldStack.isEmpty()) {
                onItemRemoved(slot, oldStack);
            }
            if (!newStack.isEmpty()) {
                onItemAdded(slot, newStack);
            }
        }
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) {
            if (isToolSlot(slot)) {
                if (player() != null) {
                    var modifiers = com.google.common.collect.ArrayListMultimap.<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>create();
                    stack.forEachModifier(net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND, modifiers::put);
                    player().getAttributes().addTransientAttributeModifiers(modifiers);
                }
                PacketSender.sendRobotInventory(this, slot, stack);
            }
            if (isUpgradeSlot(slot)) {
                PacketSender.sendRobotInventory(this, slot, stack);
            }
            if (isFloppySlot(slot)) {
                li.cil.oc.core.impl.common.Sound.playDiskInsert(this);
            }
            if (isComponentSlot(slot, stack)) {
                Computer.super.onItemAdded(slot, stack);
                var level = getLevel();
                if (level != null) {
                    level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                }
            }
            if (isInventorySlot(slot)) {
                if (machine() != null) {
                    machine().signal("inventory_changed", slot - equipmentInventory.getContainerSize() + 1);
                }
            }
        } else {
            Computer.super.onItemAdded(slot, stack);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        Computer.super.onItemRemoved(slot, stack);
        if (isServer()) {
            if (isToolSlot(slot)) {
                if (player() != null) {
                    var modifiers = com.google.common.collect.ArrayListMultimap.<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>create();
                    stack.forEachModifier(net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND, modifiers::put);
                    player().getAttributes().removeAttributeModifiers(modifiers);
                }
                PacketSender.sendRobotInventory(this, slot, ItemStack.EMPTY);
            }
            if (isUpgradeSlot(slot)) {
                PacketSender.sendRobotInventory(this, slot, ItemStack.EMPTY);
            }
            if (isFloppySlot(slot)) {
                li.cil.oc.core.impl.common.Sound.playDiskEject(this);
            }
            if (isInventorySlot(slot)) {
                if (machine() != null) {
                    machine().signal("inventory_changed", slot - equipmentInventory.getContainerSize() + 1);
                }
            }
        }
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        setItem(slot, stack);
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
    public boolean isUseableByPlayer(Player player) {
        return stillValid(player);
    }

    @Override
    public Container equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public Container mainInventory() {
        return mainInventory;
    }

    @Override
    public MultiTank tank() {
        return tank;
    }

    @Override
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(int index) {
        selectedSlot = index;
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public void setSelectedTank(int index) {
        selectedTank = index;
    }

    @Override
    public Player player() {
        if (player_ == null) {
            var l = getLevel();
            if (l instanceof ServerLevel sl) {
                player_ = createAgentPlayer(sl);
            }
        }
        return player_;
    }

    @Override
    public String name() {
        return info.name;
    }

    @Override
    public void setName(String name) {
        info.name = name;
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
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
    public List<li.cil.oc.api.network.Environment> agentComponents() {
        var result = new ArrayList<li.cil.oc.api.network.Environment>();
        for (int i = 0; i < getContainerSize(); i++) {
            var env = getComponentInSlot(i);
            if (env != null) result.add(env);
        }
        return result;
    }

    public int tankCount() {
        int count = 0;
        for (int i = 0; i < getContainerSize(); i++) {
            var env = getComponentInSlot(i);
            if (env instanceof FluidTank) count++;
        }
        return count;
    }

    public FluidTank getFluidTank(int tankIndex) {
        int idx = 0;
        for (int i = 0; i < getContainerSize(); i++) {
            var env = getComponentInSlot(i);
            if (env instanceof FluidTank t) {
                if (idx == tankIndex) return t;
                idx++;
            }
        }
        return null;
    }

    public int getTanks() {
        return tankCount();
    }

    public int getTankCapacity(int tankIndex) {
        var t = getFluidTank(tankIndex);
        return t != null ? t.getCapacity() : 0;
    }

    public int fill(li.cil.oc.core.util.FluidStack resource, boolean simulate) {
        var t = getFluidTank(selectedTank);
        return t != null ? t.fill(resource, simulate) : 0;
    }

    public li.cil.oc.core.util.FluidStack drain(li.cil.oc.core.util.FluidStack resource, boolean simulate) {
        var t = getFluidTank(selectedTank);
        if (t != null && t.getFluid().hasSameFluid(resource)) return t.drain(resource.amount(), simulate);
        return li.cil.oc.core.util.FluidStack.EMPTY;
    }

    public li.cil.oc.core.util.FluidStack drain(int maxDrain, boolean simulate) {
        var t = getFluidTank(selectedTank);
        return t != null ? t.drain(maxDrain, simulate) : li.cil.oc.core.util.FluidStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) return true;
        if (isContainerSlot(slot)) {
            var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
            if (driver != null) {
                String slotType = containerSlotType(slot);
                return driver.slot(stack).equals(slotType) && driver.tier(stack) <= containerSlotTier(slot);
            }
            return false;
        }
        return isInventorySlot(slot);
    }

    @Override
    public void connectItemNode(Node node) {
        var machineNode = machine() != null ? machine().node() : null;
        if (machineNode != null && node != null) {
            machineNode.connect(node);
        }
        if (node != null) {
            var host = node.host();
            if (host instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                for (int slot : componentSlots()) {
                    var env = getComponentInSlot(slot);
                    if (env instanceof li.cil.oc.api.internal.Keyboard kb) {
                        buffer.node().connect(kb.node());
                    } else if (env instanceof li.cil.oc.core.impl.server.component.GraphicsCard gc) {
                        buffer.node().connect(gc.node());
                    }
                }
            } else if (host instanceof li.cil.oc.api.internal.Keyboard kb) {
                for (int slot : componentSlots()) {
                    var env = getComponentInSlot(slot);
                    if (env instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                        kb.node().connect(buffer.node());
                    }
                }
            }
        }
    }

    @Override
    public void connectComponents() {
        if (isConnecting) return;
        isConnecting = true;
        try {
            if (isServer()) {
                var machineNode = machine().node();
                if (machineNode.network() == null) {
                    li.cil.oc.api.Network.joinNewNetwork(machineNode);
                }
                li.cil.oc.core.impl.common.inventory.ComponentInventory.connectComponents(this);
                if (bot instanceof li.cil.oc.core.impl.server.component.Robot rb) {
                    if (machineNode.network() != null && rb.node().network() != machineNode.network()) {
                        machineNode.connect(rb.node());
                    }
                }
            } else {
                li.cil.oc.core.impl.common.inventory.ComponentInventory.connectComponents(this);
            }
            ensureNodeAddress();
        } finally {
            isConnecting = false;
        }
    }

    @Override
    public int getSizeInventory() {
        return getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return getItem(slot);
    }

    @Override
    public void markDirty() {
        setChanged();
    }

    @Override
    public void onMessage(Message message) {
        if ("network.message".equals(message.name()) && message.source() != node()) {
            var data = message.data();
            if (data.length > 0 && data[0] instanceof Packet packet) {
                node.sendToReachable("network.message", packet);
            }
        }
    }

    @Override
    public void sendToReachable(String message, Object data) {
        if (node != null) node.sendToReachable(message, data);
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        if (side == Direction.WEST) return new int[]{0};
        if (side == Direction.EAST) return containerSlots().stream().mapToInt(i -> i).toArray();
        return inventorySlots().stream().mapToInt(i -> i).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, Direction side) {
        return Arrays.stream(getSlotsForFace(side)).anyMatch(s -> s == slot);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
        return Arrays.stream(getSlotsForFace(side)).anyMatch(s -> s == slot);
    }

    private Direction _pitch = Direction.UP;

    public Direction pitch() {
        return _pitch;
    }

    public Direction yaw() {
        return facing;
    }

    public void yaw(Direction value) {
        facing = value;
        syncFacingToBlockState();
    }

    public void pitch(Direction value) {
        _pitch = value;
        syncFacingToBlockState();
    }

    @Override
    public void facing(Direction value) {
        super.facing(value);
        syncFacingToBlockState();
    }

    public void setFromFacing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            _pitch = value;
        } else {
            _pitch = Direction.NORTH;
            facing = value;
        }
        syncFacingToBlockState();
    }

    public void setFromEntityPitchAndYaw(net.minecraft.world.entity.Entity entity) {
        Direction[] pitch2Direction = {Direction.UP, Direction.NORTH, Direction.DOWN};
        Direction[] yaw2Direction = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction newPitch = pitch2Direction[(int) Math.round(entity.getXRot() / 90.0) + 1];
        Direction newYaw = yaw2Direction[Math.round(entity.getYRot() / 360 * 4) & 3];
        _pitch = newPitch;
        facing = newYaw;
        syncFacingToBlockState();
    }

    public void invertRotation() {
        Direction newPitch = (_pitch == Direction.DOWN || _pitch == Direction.UP) ? _pitch.getOpposite() : Direction.NORTH;
        Direction newYaw = facing.getOpposite();
        _pitch = newPitch;
        facing = newYaw;
        syncFacingToBlockState();
    }

    @Override
    public Direction toLocal(Direction value) {
        return RotationHelper.toLocal(_pitch, facing, value);
    }

    @Override
    public Direction toGlobal(Direction value) {
        return RotationHelper.toGlobal(_pitch, facing, value);
    }

    public void syncFromBlockState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            var blockFacing = state.getValue(BlockStateProperties.FACING);
            if (blockFacing.getAxis().isVertical()) {
                _pitch = blockFacing;
            } else {
                facing = blockFacing;
            }
        }
    }

    public void syncFacingToBlockState() {
        var level = getLevel();
        if (level == null) return;
        var pos = getBlockPos();
        var state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            var current = state.getValue(BlockStateProperties.FACING);
            var desired = _pitch.getAxis().isVertical() ? _pitch : facing;
            if (current != desired) {
                level.setBlock(pos, state.setValue(BlockStateProperties.FACING, desired), 3);
            }
        }
    }

    public void rotate(Direction axis) {
        var level = getLevel();
        if (level == null) return;
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            var blockFacing = state.getValue(BlockStateProperties.FACING);
            var newBlockFacing = blockFacing.getClockWise(axis.getAxis());
            if (newBlockFacing != blockFacing) {
                level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.FACING, newBlockFacing), 3);
            }
            syncFromBlockState(level.getBlockState(getBlockPos()));
        }
    }

    @Override
    public void rotateProxy(Direction axis) {
        rotate(axis);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            if (pendingMachineNbt != null) {
                machine.load(pendingMachineNbt, getEffectiveProvider());
                setRunning(machine.isRunning());
                pendingMachineNbt = null;
            }
            if (machine != null) {
                machine.onHostChanged();
            }
            if (isRunning()) {
                if (EventHandlerDelegate.get() != null) EventHandlerDelegate.get().onRobotStart(this);
            }
        }
    }

    public void onBlockEntityLoad() {
        syncFromBlockState(getBlockState());
        if (isServer()) {
            if (!pendingOnLoadSkip) {
                connectComponents();
            } else {
                pendingOnLoadSkip = false;
            }
            ensureNodeAddress();
        }
    }

    @Override
    protected void onProxyMoved(net.minecraft.world.level.Level world, BlockPos newPos, BlockPos oldPos) {
        var te = world.getBlockEntity(newPos);
        if (te instanceof RobotProxy newProxy && newProxy.robot == this) {
            this.proxy = newProxy;
            this.setLevel(world);
            this.setBlockPos(newPos);
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readFromNBTForServer(nbt, provider);
        info.load(nbt, provider);
        if (nbt.contains(OCSettings.namespace + "computer")) {
            pendingMachineNbt = nbt.getCompound(OCSettings.namespace + "computer").copy();
        }
        readRobotNBT(nbt, provider);
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        saveComponents(provider);
        super.writeToNBTForServer(nbt, provider);
        if (machine != null) {
            var computerTag = new CompoundTag();
            machine.save(computerTag, provider);
            nbt.put(OCSettings.namespace + "computer", computerTag);
        }
        writeRobotNBT(nbt, provider);
        info.save(nbt, provider);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readFromNBTForClient(nbt, provider);
        info.load(nbt, provider);
        readRobotNBT(nbt, provider);
        if (isClient()) {
            clientComputerAddress = nbt.contains(OCSettings.namespace + "computerAddress")
                ? nbt.getString(OCSettings.namespace + "computerAddress")
                : null;
            connectComponents();
        }
    }

    public String clientComputerAddress;

    public String computerAddress() {
        if (clientComputerAddress != null && !clientComputerAddress.isEmpty()) return clientComputerAddress;
        var n = node();
        return (n != null && n.address() != null && !n.address().isEmpty()) ? n.address() : null;
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        saveComponents(provider);
        super.writeToNBTForClient(nbt, provider);
        writeRobotNBT(nbt, provider);
        info.save(nbt, provider);
        if (machine != null && machine.node() != null && machine.node().address() != null) {
            nbt.putString(OCSettings.namespace + "computerAddress", machine.node().address());
        }
    }

    private void readRobotNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (bot instanceof li.cil.oc.core.impl.server.component.Robot rb) {
            rb.load(nbt.getCompound(OCSettings.namespace + "robot"), provider);
        }
        if (nbt.contains(OCSettings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "pitch"));
        }
        updateInventorySize();
        if (isServer()) {
            if (machine != null) {
                machine.onHostChanged();
            }
            if (isRunning()) {
                if (EventHandlerDelegate.get() != null) EventHandlerDelegate.get().onRobotStart(this);
            }
        }
    }

    private void writeRobotNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (bot instanceof li.cil.oc.core.impl.server.component.Robot rb) {
            li.cil.oc.core.impl.util.ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "robot", t -> rb.save(t, provider));
        }
        nbt.putInt(OCSettings.namespace + "pitch", _pitch.get3DDataValue());
    }

    private static Consumer<Robot> clientDisposeCallback = robot -> {
    };

    public static void setClientDisposeCallback(Consumer<Robot> callback) {
        clientDisposeCallback = callback;
    }

    @Override
    protected void onClientDispose() {
        var level = getLevel();
        if (level != null && level.isClientSide) {
            li.cil.oc.core.impl.client.ClientRobotTracker.INSTANCE.remove(level, this);
        }
        clientDisposeCallback.accept(this);
    }
}
