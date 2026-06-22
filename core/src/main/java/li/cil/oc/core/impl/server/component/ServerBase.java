package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Machine;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.inventory.ComponentInventory;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public abstract class ServerBase implements ComponentInventory, MachineHost, ServerInventory, Analyzable, li.cil.oc.api.internal.Server, DeviceInfo {
    public final li.cil.oc.api.internal.Rack rack;
    public final int slot;
    public final li.cil.oc.api.machine.Machine machine;
    private final Map<String, String> deviceInfo;
    private ManagedEnvironment[] _components;
    private ItemStack[] _items;

    private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();

    @Override
    public ArrayList<ManagedEnvironment> updatingComponents() {
        return updatingComponents;
    }

    @Override
    public @Nullable Node node() {
        var level = rack.level();
        if (level != null && !level.isClientSide()) {
            return machine.node();
        }
        return null;
    }

    @Override
    public li.cil.oc.api.machine.Machine machine() {
        return machine;
    }

    @Override
    public int slot() {
        return slot;
    }

    @Override
    public li.cil.oc.api.internal.Rack rack() {
        return rack;
    }

    @Override
    public ItemStack[] items() {
        if (_items == null) {
            _items = new ItemStack[getContainerSize()];
        }
        return _items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        items()[slot] = stack;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void clearContent() {
        ItemStack[] itemArray = items();
        Arrays.fill(itemArray, ItemStack.EMPTY);
    }

    @Override
    public ManagedEnvironment[] _components() {
        if (_components == null) _components = new ManagedEnvironment[getContainerSize()];
        return _components;
    }

    @Override
    public void _components(ManagedEnvironment[] value) {
        _components = value;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
    }

    public boolean wasRunning = false;
    public boolean hadErrored = false;
    public long lastFileSystemAccess = 0L;
    public long lastNetworkActivity = 0L;

    private boolean isServerSide() {
        var level = rack.level();
        return level != null && !level.isClientSide();
    }

    public ServerBase(li.cil.oc.api.internal.Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
        this.machine = Machine.create(this);
        var level = rack.level();
        if (level != null && !level.isClientSide()) {
            setInfiniteBuffer();
        }
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.System, DeviceAttribute.Description, "Server", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Blader", DeviceAttribute.Capacity, String.valueOf(getContainerSize()));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public void onConnect(Node node) {
        if (node == node()) {
            connectComponents();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == node()) {
            disconnectComponents();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        ComponentInventory.super.load(nbt, provider);
        var n = node();
        if (n != null && nbt.contains("node")) {
            n.load(nbt.getCompound("node"), provider);
        }
        if (nbt.contains("machine")) {
            var level = rack.level();
            if (level == null || !level.isClientSide()) {
                machine.load(nbt.getCompound("machine"), provider);
                boolean isRunning = machine.isRunning();
                boolean hasErrored = machine.lastError() != null;
                if (isRunning != wasRunning || hasErrored != hadErrored) {
                    rack.markChanged(slot);
                }
                wasRunning = isRunning;
                hadErrored = hasErrored;
            }
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        var n = node();
        if (n != null) {
            CompoundTag nodeTag = new CompoundTag();
            n.save(nodeTag, provider);
            nbt.put("node", nodeTag);
        }
        ComponentInventory.super.save(nbt, provider);
        var level = rack.level();
        if (level != null && !level.isClientSide()) {
            CompoundTag machineNbt = new CompoundTag();
            machine.save(machineNbt, provider);
            nbt.put("machine", machineNbt);
        }
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && isComponentSlot(i, stack)) {
                list.add(stack);
            }
        }
        return list;
    }

    @Override
    public int componentSlot(String address) {
        ManagedEnvironment[] comps = componentEnvironments();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] != null && comps[i].node() != null && address.equals(comps[i].node().address()))
                return i;
        }
        return -1;
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
    public double xPosition() {
        return rack.xPosition();
    }

    @Override
    public double yPosition() {
        return rack.yPosition();
    }

    @Override
    public double zPosition() {
        return rack.zPosition();
    }

    @Override
    public net.minecraft.world.level.Level level() {
        return rack.level();
    }

    @Override
    public void markChanged() {
        rack.markChanged(slot);
    }

    @Override
    public void setChanged() {
        var level = rack.level();
        if (level != null) {
            var c = container();
            if (!c.isEmpty()) {
                CompoundTag nbt;
                var customData = c.get(DataComponents.CUSTOM_DATA);
                if (customData == null || customData.isEmpty()) {
                    nbt = new CompoundTag();
                } else {
                    nbt = customData.copyTag();
                }
                CompoundTag data = nbt.contains(Settings.namespace + "data") ? nbt.getCompound(Settings.namespace + "data") : new CompoundTag();
                save(data, level.registryAccess());
                nbt.put(Settings.namespace + "data", data);
                c.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            }
            if (rack instanceof BlockEntity be) {
                var beLevel = be.getLevel();
                if (beLevel != null) {
                    beLevel.blockEntityChanged(be.getBlockPos());
                }
            }
        }
    }

    @Override
    public abstract int tier();

    public boolean isUseableByPlayer(Player player) {
        return rack.stillValid(player);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return isUseableByPlayer(player);
    }

    @Override
    public EnvironmentHost host() {
        return rack;
    }

    @Override
    public ItemStack container() {
        return rack.getItem(slot);
    }

    @Override
    public void connectItemNode(Node node) {
        if (node != null) {
            li.cil.oc.api.Network.joinNewNetwork(machine.node());
            if (machine.node() != null) {
                machine.node().connect(node);
            }
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        ComponentInventory.super.onItemRemoved(slot, stack);
        if (isServerSide()) {
            String slotType = InventorySlots.server[tier()][slot].slot();
            if (Slot.CPU.equals(slotType)) {
                machine.stop();
            }
        }
    }

    @Override
    public CompoundTag getData() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isRunning", wasRunning);
        nbt.putBoolean("hasErrored", hadErrored);
        nbt.putLong("lastFileSystemAccess", lastFileSystemAccess);
        nbt.putLong("lastNetworkActivity", lastNetworkActivity);
        return nbt;
    }

    @Override
    public int getConnectableCount() {
        int count = 0;
        for (ManagedEnvironment env : componentEnvironments()) {
            if (env instanceof RackBusConnectable) count++;
        }
        return count;
    }

    @Override
    public @Nullable RackBusConnectable getConnectableAt(int index) {
        int idx = 0;
        for (ManagedEnvironment env : componentEnvironments()) {
            if (env instanceof RackBusConnectable) {
                if (idx == index) return (RackBusConnectable) env;
                idx++;
            }
        }
        return null;
    }

    @Override
    public abstract boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) ;

    protected abstract void setInfiniteBuffer();

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (isServerSide()) {
            machine.update();
            boolean isRunning = machine.isRunning();
            boolean hasErrored = machine.lastError() != null;
            if (isRunning != wasRunning || hasErrored != hadErrored) {
                rack.markChanged(slot);
            }
            wasRunning = isRunning;
            hadErrored = hasErrored;
            if (tier() == Tier.Four) {
                setInfiniteBuffer();
            }
        }
        updateComponents();
    }

    @Override
    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (machine.isRunning()) return EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        return EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        return new Node[]{machine.node()};
    }
}
