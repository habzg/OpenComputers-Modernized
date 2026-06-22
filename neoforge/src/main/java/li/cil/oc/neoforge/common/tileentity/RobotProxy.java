package li.cil.oc.neoforge.common.tileentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.tileentity.traits.Computer;
import li.cil.oc.core.impl.common.tileentity.traits.PowerInformation;
import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;


import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class RobotProxy extends li.cil.oc.core.impl.common.tileentity.traits.TileEntity implements Computer, PowerInformation, Rotatable, WorldlyContainer, IFluidHandler, li.cil.oc.api.internal.Robot, IHaveGoggleInformation, net.minecraft.world.Nameable {
    public final Robot robot;
    public final RobotData info;

    public RobotProxy(BlockPos pos, BlockState state) {
        this(new Robot(pos, state), pos, state);
    }

    public RobotProxy(Robot robot, BlockPos pos, BlockState state) {
        super(li.cil.oc.neoforge.common.init.TileEntities.ROBOT.get(), pos, state);
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
    public net.minecraft.network.chat.Component getCustomName() {
        return hasCustomName() ? net.minecraft.network.chat.Component.literal(info.name) : null;
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

    @Callback(doc = "function():boolean -- Starts the robot.")
    public Object[] start(Context context, Arguments args) {
        return (Object[]) result(!machine().isPaused() && machine().start());
    }

    @Callback(doc = "function():boolean -- Stops the robot.")
    public Object[] stop(Context context, Arguments args) {
        return (Object[]) result(machine().stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the robot is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return (Object[]) result(machine().isRunning());
    }

    @Callback(doc = "function(name:string):string -- Sets a new name.")
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
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        if (robot.getLevel() == null) {
            var level = getLevel();
            if (level != null) robot.setLevel(level);
            robot.setBlockPos(getBlockPos());
            robot.proxy = this;
        }
        super.handleUpdateTag(tag, provider);
    }

    @Override
    public void initialize() {
        if (isServer()) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        boolean firstProxy = robot.proxy == null;
        robot.proxy = this;
        var level = getLevel();
        if (level != null) robot.setLevel(level);
        robot.setBlockPos(getBlockPos());
        if (firstProxy) {
            robot.onLoad();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (robot.proxy == this) {
            robot.setRemoved();
        }
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
        robot.readFromNBTForClient(nbt, getEffectiveProvider());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        robot.setChanged();
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
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
    public int getTanks() {
        return robot.getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return robot.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return robot.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return robot.isFluidValid(tank, stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return robot.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return robot.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        return robot.drain(maxDrain, action);
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
    public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
        return robot.getStackInSlot(slot);
    }

    @Override
    public boolean isComponentSlot(int slot, net.minecraft.world.item.ItemStack stack) {
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
    public java.lang.Iterable<net.minecraft.world.item.ItemStack> internalComponents() {
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return robot.addToGoggleTooltip(tooltip, isPlayerSneaking);
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
    public void spawnStackInWorld(net.minecraft.world.item.ItemStack stack) {
        robot.spawnStackInWorld(stack);
    }

    @Override
    public void spawnStackInWorld(net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction direction) {
        robot.spawnStackInWorld(stack, direction);
    }

    @Override
    public void dropSlot(int slot) {
        robot.dropSlot(slot);
    }

    @Override
    public void dropSlot(int slot, int count, net.minecraft.core.Direction direction) {
        robot.dropSlot(slot, count, direction);
    }

    @Override
    public void dropAllSlots() {
        robot.dropAllSlots();
    }

    @Override
    public void updateItems(int slot, net.minecraft.world.item.ItemStack stack) {
        robot.updateItems(slot, stack);
    }

    @Override
    public net.minecraft.world.item.ItemStack[] items() {
        return robot.items();
    }

    @Override
    public li.cil.oc.api.network.EnvironmentHost host() {
        return robot.host();
    }

    @Override
    public java.util.ArrayList<li.cil.oc.api.network.ManagedEnvironment> updatingComponents() {
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
