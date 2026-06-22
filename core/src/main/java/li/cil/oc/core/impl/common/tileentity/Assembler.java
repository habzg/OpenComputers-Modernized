package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.template.AssemblerTemplates;
import li.cil.oc.core.impl.common.tileentity.traits.Inventory;
import li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Assembler extends TileEntity implements li.cil.oc.api.network.Environment, PowerAcceptor, Inventory, SidedEnvironment, li.cil.oc.api.util.StateAware, DeviceInfo {
    public static BlockEntityType<Assembler> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("assembler")
            .withConnector(Settings.get().bufferConverter)
            .create();
    private final ItemStack[] _items = new ItemStack[getContainerSize()];
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Assembler",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Factorizer R1D1"
    );
    public ItemStack output = null;
    public double totalRequiredEnergy = 0;
    public double requiredEnergy = 0;

    public Assembler(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public Node node() {
        return node;
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

    public void onNeighborChanged() {
        ae2OnNeighborChanged();
    }

    public Object[] result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean canConnect(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public Node sidedNode(Direction side) {
        return side != Direction.UP ? node : null;
    }

    protected boolean hasConnector(Direction side) {
        return canConnect(side);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().assemblerRate;
    }

    @Override
    public double globalBufferSize(Direction side) {
        if (node instanceof li.cil.oc.api.network.Connector c && hasConnector(side)) {
            return c.globalBufferSize();
        }
        return 0;
    }

    @Override
    public double globalDemand(Direction side) {
        if (node instanceof li.cil.oc.api.network.Connector c && hasConnector(side)) {
            return Math.clamp(c.globalBufferSize() - c.globalBuffer(), 0, energyThroughput());
        }
        return 0;
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        if (node instanceof li.cil.oc.api.network.Connector c) {
            if (c.tryChangeBuffer(amount)) return amount;
            else return 0;
        }
        return 0;
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return hasConnector(side);
    }

    @Override
    public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (isAssembling()) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        else if (canAssemble()) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        else return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    public boolean canAssemble() {
        var template = AssemblerTemplates.select(getItem(0));
        return template != null && !isAssembling() && output == null && template.validate(this).valid();
    }

    public boolean isAssembling() {
        return requiredEnergy > 0;
    }

    public double progress() {
        return (1 - requiredEnergy / totalRequiredEnergy) * 100;
    }

    public int timeRemaining() {
        return (int) (requiredEnergy / Settings.get().assemblerTickAmount / 20);
    }

    public synchronized boolean start(boolean finishImmediately) {
        var template = AssemblerTemplates.select(getItem(0));
        if (template != null && !isAssembling() && output == null && template.validate(this).valid()) {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                var stack = getItem(slot);
                if (stack.isEmpty()) continue;
                if (!canPlaceItem(slot, stack)) return false;
            }
            var result = template.assemble(this);
            output = (ItemStack) result[0];
            double energy = (double) result[1];
            totalRequiredEnergy = finishImmediately ? 0 : Math.max(1, energy);
            requiredEnergy = totalRequiredEnergy;
            PacketSender.sendRobotAssembling(this, true);
            for (int slot = 0; slot < getContainerSize(); slot++) setItem(slot, ItemStack.EMPTY);
            setChanged();
            return true;
        }
        return false;
    }

    @Callback(doc = "function(): string, number or boolean -- The current state of the assembler.")
    public Object[] status(Context context, Arguments args) {
        if (isAssembling()) return result("busy", progress());
        var template = AssemblerTemplates.select(getItem(0));
        if (template != null && template.validate(this).valid()) return result("idle", true);
        return result("idle", false);
    }

    @Callback(doc = "function():boolean -- Start assembling, if possible.")
    public Object[] start(Context context, Arguments args) {
        return result(start(false));
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) node.remove();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        var level = getLevel();
        if (output != null && level != null && level.getGameTime() % Settings.get().tickFrequency == 0) {
            double want = Math.clamp(requiredEnergy, 1, Settings.get().assemblerTickAmount * Settings.get().tickFrequency);
            double have = want + (Settings.get().ignorePower ? 0 : ((li.cil.oc.api.network.Connector) node).changeBuffer(-want));
            requiredEnergy -= have;
            if (requiredEnergy <= 0) {
                setItem(0, output);
                output = null;
                requiredEnergy = 0;
            }
            PacketSender.sendRobotAssembling(this, have > 0.5 && output != null);
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        if (nbt.contains(Settings.namespace + "output")) {
            output = ItemStack.parseOptional(getEffectiveProvider(), nbt.getCompound(Settings.namespace + "output"));
        } else if (nbt.contains(Settings.namespace + "robot")) {
            output = ItemStack.parseOptional(getEffectiveProvider(), nbt.getCompound(Settings.namespace + "robot"));
        }
        totalRequiredEnergy = nbt.getDouble(Settings.namespace + "total");
        requiredEnergy = nbt.getDouble(Settings.namespace + "remaining");
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        if (output != null) {
            var saved = new CompoundTag();
            output.save(getEffectiveProvider(), saved);
            nbt.put(Settings.namespace + "output", saved);
        }
        nbt.putDouble(Settings.namespace + "total", totalRequiredEnergy);
        nbt.putDouble(Settings.namespace + "remaining", requiredEnergy);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        requiredEnergy = nbt.getDouble("remaining");
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putDouble("remaining", requiredEnergy);
    }

    @Override
    public ItemStack[] items() {
        return _items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        _items[slot] = stack;
    }

    @Override
    public int getContainerSize() {
        return 22;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) return !isAssembling() && AssemblerTemplates.select(stack) != null;
        var template = AssemblerTemplates.select(getItem(0));
        if (template == null) return false;
        var tplSlot = AssemblerTemplates.NoSlot;
        if (slot >= 1 && slot < 4) tplSlot = template.containerSlots()[slot - 1];
        else if (slot >= 4 && slot < 13) tplSlot = template.upgradeSlots()[slot - 4];
        else if (slot >= 13 && slot < 21) tplSlot = template.componentSlots()[slot - 13];
        return tplSlot.validate(this, slot, stack);
    }

    @Override
    public void spawnStackInWorld(ItemStack stack) {
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction direction) {
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
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
    public double globalBuffer(Direction side) {
        if (node instanceof li.cil.oc.api.network.Connector c && hasConnector(side)) {
            return c.globalBuffer();
        }
        return 0;
    }
}
