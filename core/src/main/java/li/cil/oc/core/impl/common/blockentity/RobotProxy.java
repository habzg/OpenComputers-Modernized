package li.cil.oc.core.impl.common.blockentity;

import java.util.EnumSet;
import java.util.UUID;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientRobotTracker;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.impl.common.blockentity.traits.PowerInformation;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RobotProxy extends BlockEntity implements Computer, PowerInformation, Rotatable, WorldlyContainer, li.cil.oc.api.internal.Robot, net.minecraft.world.Nameable {
  public Robot robot;
    public RobotData info;

    public RobotProxy(BlockEntityType<?> type, Robot robot, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.robot = robot;
        this.info = robot.info;
    }

    @Override
    public HolderLookup.Provider getEffectiveProvider() {
        if (getLevel() != null) return getLevel().registryAccess();
        return super.getEffectiveProvider();
    }

    @Override
    public Node node() {
        return robot.node();
    }

    @Override
    public Machine machine() {
        return robot.machine();
    }

    @Override
    public int tier() {
        return robot.tier();
    }

    @Override
    public Container equipmentInventory() {
        return robot.equipmentInventory();
    }

    @Override
    public Container mainInventory() {
        return robot.mainInventory();
    }

    @Override
    public li.cil.oc.api.internal.MultiTank tank() {
        return robot.tank();
    }

    @Override
    public int selectedSlot() {
        return robot.selectedSlot();
    }

    @Override
    public void setSelectedSlot(int index) {
        robot.setSelectedSlot(index);
    }

    @Override
    public int selectedTank() {
        return robot.selectedTank();
    }

    @Override
    public void setSelectedTank(int index) {
        robot.setSelectedTank(index);
    }

    @Override
    public Player player() {
        return robot.player();
    }

    @Override
    public String name() {
        return robot.name();
    }

    @Override
    public void setName(String name) {
        robot.setName(name);
    }

    @Override
    public @NotNull Component getName() {
        return hasCustomName() ? Component.literal(info.name) : Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public boolean hasCustomName() {
        return !info.name.isEmpty();
    }

    @Override
    public Component getCustomName() {
        return hasCustomName() ? Component.literal(info.name) : null;
    }

    @Override
    public String ownerName() {
        return robot.ownerName();
    }

    @Override
    public UUID ownerUUID() {
        return robot.ownerUUID();
    }

    @Override
    public void connectComponents() {}

    @Override
    public void disconnectComponents() {}

    @Override
    public boolean isRunning() {
        return robot.isRunning();
    }

    @Override
    public void setRunning(boolean value) {
        robot.setRunning(value);
    }

    @Override
    public boolean shouldAnimate() {
        return robot.shouldAnimate();
    }

    @Override
    public int componentCount() {
        return robot.componentCount();
    }

    @Override
    public ManagedEnvironment getComponentInSlot(int index) {
        return robot.getComponentInSlot(index);
    }

    @Override
    public void synchronizeSlot(int slot) {
        robot.synchronizeSlot(slot);
    }

    @Callback(doc = "function():boolean -- Starts the robot. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) {
        return (Object[]) result(!machine().isPaused() && machine().start());
    }

    @Callback(doc = "function():boolean -- Stops the robot. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) {
        return (Object[]) result(machine().stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the robot is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return (Object[]) result(machine().isRunning());
    }

    @Callback(doc = "function(name: string):string -- Sets a new name and returns the old name. Robot must not be running")
    public Object[] setName(Context context, Arguments args) {
        var oldName = robot.name();
        var newName = args.checkString(0);
        if (machine().isRunning()) return (Object[]) result(null, "is running");
        robot.setName(newName);
        PacketSender.sendRobotNameChange(this, robot.name());
        return (Object[]) result(oldName);
    }

    @Callback(doc = "function():string -- Returns the robot name.")
    public Object[] getName(Context context, Arguments args) {
        return (Object[]) result(robot.name());
    }

    @Override
    public void onMessage(Message message) {
        robot.onMessage(message);
    }

    @Override
    public void updateEntity() {
        robot.updateEntity();
    }

    @Override
    public void initialize() {
        super.initialize();
        setupRobotProxy();
        if (isServer()) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    protected void setupRobotProxy() {
        boolean firstProxy = robot.proxy == null;
        robot.proxy = this;
        var level = getLevel();
        if (level != null) robot.setLevel(level);
        robot.setBlockPos(getBlockPos());
        if (firstProxy) {
            robot.onBlockEntityLoad();
        }
        if (isServer()) {
            var address = robot.computerAddress();
            if (address != null) {
                li.cil.oc.core.impl.server.ServerRobotRegistry.INSTANCE.put(level, address, this);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (robot.proxy == this) {
            var level = getLevel();
            if (level != null && level.isClientSide) {
                robot.proxy = null;
                var oldPos = getBlockPos();
                li.cil.oc.core.impl.util.ClientTickScheduler.schedule(() -> disposeOrphanedRobot(oldPos));
            } else {
                robot.setRemoved();
                var addr = robot.computerAddress();
                if (addr != null) {
                    li.cil.oc.core.impl.server.ServerRobotRegistry.INSTANCE.remove(getLevel(), addr, this);
                }
            }
        }
    }

    private void disposeOrphanedRobot(BlockPos oldPos) {
        if (robot.proxy != null) {
            return;
        }
        var level = getLevel();
        if (level != null && level.isClientSide && isAfterimageAt(oldPos)) {
            li.cil.oc.core.impl.util.ClientTickScheduler.schedule(() -> disposeOrphanedRobot(oldPos));
            return;
        }
        if (robot instanceof li.cil.oc.core.impl.common.inventory.ComponentInventory ci) {
            ci.disconnectComponents();
        }
        var node = robot.node();
        if (node != null) node.remove();
        if (level != null && level.isClientSide) {
            ClientRobotTracker.INSTANCE.remove(level, robot);
        }
        robot.setRemoved();
    }

    private void adoptOrphanedRobot(Robot existing, BlockPos pos) {
        var fresh = robot;
        if (fresh instanceof li.cil.oc.core.impl.common.inventory.ComponentInventory ci) {
            ci.disconnectComponents();
        }
        var node = fresh.node();
        if (node != null) node.remove();
        fresh.setRemoved();
        robot = existing;
        info = existing.info;
        existing.proxy = this;
        var level = getLevel();
        if (level != null) existing.setLevel(level);
        existing.setBlockPos(pos);
    }

    private boolean tryAdoptOrphanedRobot(CompoundTag nbt) {
        var level = getLevel();
        if (level == null || !level.isClientSide) return false;
        if (!nbt.contains(OCSettings.namespace + "computerAddress")) {
            return false;
        }
        var address = nbt.getString(OCSettings.namespace + "computerAddress");
        var existing = ClientRobotTracker.INSTANCE.get(level, address);
        if (existing == null || existing == robot || existing.proxy != null) return false;
        adoptOrphanedRobot(existing, getBlockPos());
        return true;
    }

    private void registerInClientRobotTracker() {
        var level = getLevel();
        if (level == null || !level.isClientSide) return;
        var address = robot.clientComputerAddress;
        if (address != null && !address.isEmpty()) {
            ClientRobotTracker.INSTANCE.add(level, address, robot);
        }
    }

    private boolean isAfterimageAt(BlockPos pos) {
        var level = getLevel();
        if (level == null) return false;
        return level.getBlockState(pos).getBlock() instanceof li.cil.oc.core.impl.common.block.RobotAfterimage;
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        robot.readFromNBTForServer(nbt, getEffectiveProvider());
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        robot.writeToNBTForServer(nbt, getEffectiveProvider());
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        robot.writeToNBTForClient(nbt, getEffectiveProvider());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (!tryAdoptOrphanedRobot(nbt)) {
            robot.readFromNBTForClient(nbt, getEffectiveProvider());
        }
        registerInClientRobotTracker();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        robot.setChanged();
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return robot.onAnalyze(player, side, hitX, hitY, hitZ);
    }

    @Override
    public double energyThroughput() {
        return robot.energyThroughput();
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return robot.canConnectPower(side);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return robot.tryChangeBuffer(side, amount);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        return robot.tryChangeBuffer(side, amount, doReceive);
    }

    @Override
    public double globalBuffer(Direction side) {
        return robot.globalBuffer(side);
    }

    @Override
    public double globalBufferSize(Direction side) {
        return robot.globalBufferSize(side);
    }

    @Override
    public double globalDemand(Direction side) {
        return robot.globalDemand(side);
    }

    @Override
    public double globalBuffer() {
        return robot.globalBuffer();
    }

    @Override
    public void globalBuffer(double value) {
        robot.globalBuffer(value);
    }

    @Override
    public double globalBufferSize() {
        return robot.globalBufferSize();
    }

    @Override
    public void globalBufferSize(double value) {
        robot.globalBufferSize(value);
    }

    @Override
    public void updatePowerInformation() {
        robot.updatePowerInformation();
    }

    @Override
    public Direction facing() {
        return robot.facing();
    }

    @Override
    public void facing(Direction value) {
        robot.facing(value);
    }

    @Override
    public Direction pitch() {
        return robot.pitch();
    }

    @Override
    public void pitch(Direction value) {
        robot.pitch(value);
    }

    @Override
    public Direction yaw() {
        return robot.yaw();
    }

    @Override
    public void yaw(Direction value) {
        robot.yaw(value);
    }

    @Override
    public Direction toGlobal(Direction value) {
        return robot.toGlobal(value);
    }

    @Override
    public Direction toLocal(Direction value) {
        return robot.toLocal(value);
    }

    @Override
    public int getContainerSize() {
        return robot.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return robot.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int index) {
        return robot.getItem(index);
    }

    @Override
    public @NotNull ItemStack removeItem(int index, int count) {
        return robot.removeItem(index, count);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index) {
        return robot.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        robot.setItem(index, stack);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return robot.stillValid(player);
    }

    @Override
    public void clearContent() {
        robot.clearContent();
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return robot.getSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, @NotNull ItemStack itemStack, Direction direction) {
        return robot.canPlaceItemThroughFace(index, itemStack, direction);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, @NotNull ItemStack stack, @NotNull Direction direction) {
        return robot.canTakeItemThroughFace(index, stack, direction);
    }

    @Override
    public net.minecraft.world.level.Level level() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return robot.xPosition();
    }

    @Override
    public double yPosition() {
        return robot.yPosition();
    }

    @Override
    public double zPosition() {
        return robot.zPosition();
    }

    @Override
    public void markChanged() {
        robot.markChanged();
    }

    public void setAnimateSwing(int ticks) {
        robot.setAnimateSwing(ticks);
    }

    public void setAnimateTurn(int axis, int ticks) {
        robot.setAnimateTurn(axis, ticks);
    }

    public void setAnimateMove(li.cil.oc.core.impl.util.BlockPosition from, int ticks) {
        robot.setAnimateMove(from, ticks);
    }

    public void move(Direction direction) {
        robot.move(direction);
    }

    @Override
    public boolean isConnected() {
        return robot.isConnected();
    }

    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public boolean hasErrored() {
        return robot.hasErrored();
    }

    @Override
    public void hasErrored(boolean value) {
        robot.hasErrored(value);
    }

    @Override
    public boolean canInteract(String player) {
        return robot.canInteract(player);
    }

    @Override
    public void setUsers(Iterable<String> list) {
        robot.setUsers(list);
    }

    @Override
    public boolean hasRedstoneCard() {
        return robot.hasRedstoneCard();
    }

    @Override
    public java.util.Set<String> users() {
        return robot.users();
    }

    @Override
    public int getSizeInventory() {
        return robot.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return robot.getStackInSlot(slot);
    }

    @Override
    public boolean isComponentSlot(int slot, ItemStack stack) {
        return robot.isComponentSlot(slot, stack);
    }

    @Override
    public void onMachineConnect(Node node) {
        robot.onMachineConnect(node);
    }

    @Override
    public void onMachineDisconnect(Node node) {
        robot.onMachineDisconnect(node);
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return robot.internalComponents();
    }

    @Override
    public Iterable<ManagedEnvironment> installedComponents() {
        return robot.installedComponents();
    }

    @Override
    public void markDirty() {
        robot.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        return robot.isUseableByPlayer(player);
    }

    @Override
    public void onRotationChanged() {
        robot.onRotationChanged();
    }

    @Override
    public void checkRedstoneInputChanged() {
        robot.checkRedstoneInputChanged();
    }

    @Override
    public void onConnect(Node node) {
        robot.onConnect(node);
    }

    @Override
    public void onDisconnect(Node node) {
        robot.onDisconnect(node);
    }

    @Override
    public ItemStack[] pendingRemovals() {
        return robot.pendingRemovals();
    }

    @Override
    public ItemStack[] pendingAdds() {
        return robot.pendingAdds();
    }

    @Override
    public int x() {
        return robot.x();
    }

    @Override
    public int y() {
        return robot.y();
    }

    @Override
    public int z() {
        return robot.z();
    }

    @Override
    public void spawnStackInWorld(ItemStack stack) {
        robot.spawnStackInWorld(stack);
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction direction) {
        robot.spawnStackInWorld(stack, direction);
    }

    @Override
    public void dropSlot(int slot) {
        robot.dropSlot(slot);
    }

    @Override
    public void dropSlot(int slot, int count, Direction direction) {
        robot.dropSlot(slot, count, direction);
    }

    @Override
    public void dropAllSlots() {
        robot.dropAllSlots();
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        robot.updateItems(slot, stack);
    }

    @Override
    public ItemStack[] items() {
        return robot.items();
    }

    @Override
    public li.cil.oc.api.network.EnvironmentHost host() {
        return robot.host();
    }

    @Override
    public java.util.ArrayList<ManagedEnvironment> updatingComponents() {
        return robot.updatingComponents();
    }

    @Override
    public boolean isSizeInventoryReady() {
        return robot.isSizeInventoryReady();
    }

    @Override
    public ManagedEnvironment[] _components() {
        return robot._components();
    }

    @Override
    public void _components(ManagedEnvironment[] value) {
        robot._components(value);
    }

    @Override
    public Object[] getInterfaces(int side) {
        return robot.getInterfaces(side);
    }

    @Override
    public int componentSlot(String address) {
        return robot.componentSlot(address);
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return robot.getCurrentState();
    }
}
