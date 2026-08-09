package li.cil.oc.core.impl.common.blockentity;

import com.google.common.base.Charsets;
import dan200.computercraft.api.peripheral.IComputerAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.Memory;
import li.cil.oc.core.impl.common.blockentity.traits.ComponentInventory;
import li.cil.oc.core.impl.common.blockentity.traits.HubBlockEntity;
import li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.blockentity.traits.SwitchLike;
import li.cil.oc.core.impl.integration.opencomputers.DriverLinkedCard;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.integration.ModIDs;
import li.cil.oc.core.server.network.QuantumNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Relay extends HubBlockEntity implements ComponentInventory, PowerAcceptor, Analyzable, WirelessEndpoint, QuantumNetwork.QuantumNode, SwitchLike {
    public static BlockEntityType<?> TYPE;

    public final Node[] componentNodes = new Node[6];
    private final ItemStack[] items = new ItemStack[getContainerSize()];
    private final ItemStack[] pendingRemovalItems;
    private final ItemStack[] pendingAddItems;
    private ManagedEnvironment[] _components;
    private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();
    public int wirelessTier = -1;
    public double strength = 0;
    public boolean isRepeater = true;
    public boolean isLinkedEnabled = false;
    public String tunnel = "creative";

    public Relay(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < 6; i++) {
            componentNodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                    .withComponent("relay")
                    .create();
        }
        pendingRemovalItems = new ItemStack[items.length];
        pendingAddItems = new ItemStack[items.length];
        Arrays.fill(items, ItemStack.EMPTY);
    }

    public Relay(BlockPos pos, BlockState state) {
        this(TYPE, pos, state);
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
        if (isServer()) {
            for (var node : componentNodes) {
                if (node != null) node.remove();
            }
        }
    }

    public void onNeighborChanged() {
        ae2OnNeighborChanged();
    }

    public EnvironmentHost host() {
        return this;
    }

    public ManagedEnvironment[] _components() {
        return _components;
    }

    public void _components(ManagedEnvironment[] value) {
        _components = value;
    }

    public boolean isSizeInventoryReady() {
        return true;
    }

    public ArrayList<ManagedEnvironment> updatingComponents() {
        return updatingComponents;
    }

    public Level level() {
        return getLevel();
    }

    public int x() {
        return worldPosition.getX();
    }

    public int y() {
        return worldPosition.getY();
    }

    public int z() {
        return worldPosition.getZ();
    }

    @Override
    public int relayDelay() {
        return relayDelay;
    }

    public final ArrayList<Object> computers = new ArrayList<>();
    public final Map<Object, Set<Integer>> openPorts = new HashMap<>();

    public boolean isWirelessEnabled() {
        return wirelessTier >= Tier.One;
    }

    public boolean isLinkedEnabled() {
        return isLinkedEnabled;
    }

    public double maxWirelessRange() {
        return wirelessTier >= Tier.One && wirelessTier <= Tier.Two ? OCSettings.get().maxWirelessRange[wirelessTier] : 0;
    }

    public double wirelessCostPerRange() {
        return wirelessTier >= Tier.One && wirelessTier <= Tier.Two ? OCSettings.get().wirelessCostPerRange[wirelessTier] : 0;
    }

    @Override
    public boolean canConnect(Direction side) {
        return true;
    }

    @Override
    public Node sidedNode(Direction side) {
        return super.sidedNode(side);
    }

    protected Connector connector(Direction side) {
        var n = sidedNode(side);
        return n instanceof Connector c ? c : null;
    }

    public boolean canConnectPower(Direction side) {
        return true;
    }

    public double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        var c = connector(side);
        if (c != null) {
            if (c.tryChangeBuffer(amount)) return amount;
        }
        return 0;
    }

    public double globalBuffer(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBuffer() : 0.0;
    }

    public double globalBufferSize(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBufferSize() : 0.0;
    }

    public double globalDemand(Direction side) {
        var c = connector(side);
        return c != null ? Math.clamp(c.globalBufferSize() - c.globalBuffer(), 0, energyThroughput()) : 0.0;
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().accessPointRate;
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        if (isWirelessEnabled())
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.wirelessstrength", String.valueOf((int) strength)), false);
        return new Node[]{componentNodes[side.ordinal()]};
    }

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when relaying messages.")
    public synchronized Object[] getStrength(Context ignoredContext, Arguments ignoredArgs) {
        return li.cil.oc.core.util.ResultWrapper.result(strength);
    }

    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when relaying messages.")
    public synchronized Object[] setStrength(Context ignoredContext, Arguments args) {
        strength = Math.clamp(args.checkDouble(0), 0, maxWirelessRange());
        return li.cil.oc.core.util.ResultWrapper.result(strength);
    }

    @Callback(direct = true, doc = "function():boolean -- Get whether the access point currently acts as a repeater (resend received wireless packets wirelessly).")
    public synchronized Object[] isRepeater(Context ignoredContext, Arguments ignoredArgs) {
        return li.cil.oc.core.util.ResultWrapper.result(isRepeater);
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Set whether the access point should act as a repeater.")
    public synchronized Object[] setRepeater(Context ignoredContext, Arguments args) {
        isRepeater = args.checkBoolean(0);
        return li.cil.oc.core.util.ResultWrapper.result(isRepeater);
    }

    @Override
    public void receivePacket(Packet packet, WirelessEndpoint source) {
        if (isWirelessEnabled()) tryEnqueuePacket(null, packet);
    }

    @Override
    public void receivePacket(Packet packet) {
        if (isLinkedEnabled()) tryEnqueuePacket(null, packet);
    }

    @Override
    public String tunnel() {
        return tunnel;
    }

    @Override
    public boolean tryEnqueuePacket(Direction sourceSide, Packet packet) {
        onEnqueuePacket(packet);
        return super.tryEnqueuePacket(sourceSide, packet);
    }

    protected void onEnqueuePacket(Packet packet) {
        if (ModIDs.isModLoaded(ModIDs.ComputerCraft)) {
            var data = packet.data();
            if (data.length > 0 && data[0] instanceof Double answerPort) {
                var rest = new Object[data.length - 1];
                System.arraycopy(data, 1, rest, 0, data.length - 1);
                queueCCMessage(packet.source(), packet.destination(), packet.port(), answerPort.intValue(), rest);
            } else {
                queueCCMessage(packet.source(), packet.destination(), packet.port(), -1, data);
            }
        }
    }

    private void queueCCMessage(String source, String destination, int port, int answerPort, Object[] args) {
        for (var obj : computers) {
            if (obj instanceof IComputerAccess computer) {
                String address = "cc" + computer.getID() + "_" + computer.getAttachmentName();
                if (!source.equals(address) && (destination == null || destination.equals(address)) && openPorts.getOrDefault(computer, java.util.Set.of()).contains(port)) {
                    var eventArgs = new Object[3 + args.length];
                    eventArgs[0] = computer.getAttachmentName();
                    eventArgs[1] = port;
                    eventArgs[2] = answerPort;
                    System.arraycopy(args, 0, eventArgs, 3, args.length);
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof byte[]) {
                            eventArgs[3 + i] = new String((byte[]) args[i], Charsets.UTF_8);
                        }
                    }
                    computer.queueEvent("modem_message", eventArgs);
                }
            }
        }
    }

    @Override
    protected void relayPacket(Direction sourceSide, Packet packet) {
        super.relayPacket(sourceSide, packet);
        java.util.function.Function<Double, Boolean> tryChangeBuffer = sourceSide != null
                ? (amount) -> plugs[sourceSide.ordinal()].node() instanceof Connector c && c.tryChangeBuffer(amount)
                : (amount) -> {
            for (var plug : plugs)
                if (plug.node() instanceof Connector c && c.tryChangeBuffer(amount)) return true;
            return false;
        };
        if (isWirelessEnabled() && strength > 0 && (sourceSide != null || isRepeater)) {
            double cost = wirelessCostPerRange();
            if (tryChangeBuffer.apply(-strength * cost)) {
                li.cil.oc.api.Network.sendWirelessPacket(this, strength, packet);
            }
        }
        if (isLinkedEnabled() && sourceSide != null) {
            double cost = packet.size() / 32.0 + wirelessCostPerRange() * maxWirelessRange() * 5;
            if (tryChangeBuffer.apply(-cost)) {
                for (var endpoint : QuantumNetwork.getEndpoints(tunnel)) {
                    if (endpoint != this) endpoint.receivePacket(packet);
                }
            }
        }
        onSwitchActivity();
    }

    @Override
    public Node createNode(Plug plug) {
        return li.cil.oc.api.Network.newNode(plug, Visibility.Network)
                .withConnector(Math.round(OCSettings.get().bufferAccessPoint))
                .create();
    }

    @Override
    public void onPlugConnect(Plug plug, Node node) {
        super.onPlugConnect(plug, node);
        if (node == plug.node()) li.cil.oc.api.Network.joinWirelessNetwork(this);
        if (plug.isPrimary()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
    }

    @Override
    public void onPlugDisconnect(Plug plug, Node node) {
        super.onPlugDisconnect(plug, node);
        if (node == plug.node()) li.cil.oc.api.Network.leaveWirelessNetwork(this);
        if (plug.isPrimary() && node != plug.node()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
    }

    @Override
    public void connectComponents() {
        ComponentInventory.super.connectComponents();
    }

    @Override
    public void disconnectComponents() {
        ComponentInventory.super.disconnectComponents();
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        updateLimits(slot, stack);
    }

    private void updateLimits(int ignoredSlot, ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
        if (driver != null) {
            if (driver.slot(stack).equals(Slot.CPU)) {
                relayDelay = Math.max(1, (int) (relayBaseDelay - (driver.tier(stack) + 1) * relayDelayPerUpgrade));
            } else if (driver.slot(stack).equals(Slot.Memory)) {
                var item = stack.getItem();
                if (item instanceof Memory ram) {
                    relayAmount = Math.max(1, relayBaseAmount + ((ram.tier() + 1) * relayAmountPerUpgrade));
                } else {
                    relayAmount = Math.max(1, relayBaseAmount + ((driver.tier(stack) + 1) * relayAmountPerUpgrade * 2));
                }
            } else if (driver.slot(stack).equals(Slot.HDD)) {
                maxQueueSize = Math.max(1, queueBaseSize + (driver.tier(stack) + 1) * queueSizePerUpgrade);
            } else if (driver.slot(stack).equals(Slot.Card)) {
                var descriptor = li.cil.oc.api.Items.get(stack);
                if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier1) || descriptor == li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier2)) {
                    wirelessTier = descriptor == li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier1) ? Tier.One : Tier.Two;
                }
                if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.LinkedCard)) {
                    var data = DriverLinkedCard.getDataTag(stack);
                    if (data.contains(OCSettings.namespace + "tunnel")) {
                        tunnel = data.getString(OCSettings.namespace + "tunnel");
                        isLinkedEnabled = true;
                        QuantumNetwork.add(this);
                    }
                }
            }
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
        if (driver != null) {
            if (driver.slot(stack).equals(Slot.CPU)) relayDelay = relayBaseDelay;
            else if (driver.slot(stack).equals(Slot.Memory)) relayAmount = relayBaseAmount;
            else if (driver.slot(stack).equals(Slot.HDD)) maxQueueSize = queueBaseSize;
            else if (driver.slot(stack).equals(Slot.Card)) {
                wirelessTier = -1;
                isLinkedEnabled = false;
                QuantumNetwork.remove(this);
            }
        }
    }

    @Override
    public int getContainerSize() {
        return InventorySlots.relay.length;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
        if (driver == null) return false;
        var provided = InventorySlots.relay[slot];
        boolean tierSatisfied = driver.slot(stack).equals(provided.slot()) && driver.tier(stack) <= provided.tier();
        boolean cardTypeSatisfied = true;
        if (provided.slot().equals(Slot.Card)) {
            var desc = li.cil.oc.api.Items.get(stack);
            cardTypeSatisfied = desc == li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier1) ||
                    desc == li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier2) ||
                    desc == li.cil.oc.api.Items.get(Constants.ItemName.LinkedCard);
        }
        return tierSatisfied && cardTypeSatisfied;
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] != null) updateLimits(slot, items[slot]);
        }
        if (nbt.contains(OCSettings.namespace + "strength")) {
            strength = Math.clamp(nbt.getDouble(OCSettings.namespace + "strength"), 0, maxWirelessRange());
        }
        if (nbt.contains(OCSettings.namespace + "isRepeater")) {
            isRepeater = nbt.getBoolean(OCSettings.namespace + "isRepeater");
        }
        var tagList = nbt.getList(OCSettings.namespace + "componentNodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(tagList.size(), componentNodes.length); i++) {
            var provider = getEffectiveProvider();
            if (provider != null) componentNodes[i].load(tagList.getCompound(i), provider);
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        nbt.putDouble(OCSettings.namespace + "strength", strength);
        nbt.putBoolean(OCSettings.namespace + "isRepeater", isRepeater);
        var tagList = new ListTag();
        for (var n : componentNodes) {
            var tag = new CompoundTag();
            n.save(tag, getEffectiveProvider());
            tagList.add(tag);
        }
        nbt.put(OCSettings.namespace + "componentNodes", tagList);
    }

    @Override
    public ItemStack[] items() {
        return items;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    public Container inventory() {
        return null;
    }

    public boolean isUseableByPlayer(Player player) {
        return true;
    }

    public void dropSlot(int slot) {
        dropSlot(slot, 1, null);
    }

    public void dropSlot(int slot, int count, Direction direction) {
        var stack = getItem(slot);
        if (!stack.isEmpty()) {
            var toDrop = stack.split(count);
            if (direction != null) spawnStackInWorld(toDrop, direction);
            else spawnStackInWorld(toDrop);
        }
    }

    public void dropAllSlots() {
        for (int i = 0; i < getContainerSize(); i++) {
            dropSlot(i);
        }
    }

    public void clearContent() {
        Arrays.fill(items, ItemStack.EMPTY);
    }

    public void spawnStackInWorld(ItemStack stack) {
    }

    public void spawnStackInWorld(ItemStack stack, Direction direction) {
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        items[slot] = stack;
    }

    @Override
    public ItemStack[] pendingRemovals() {
        return pendingRemovalItems;
    }

    @Override
    public ItemStack[] pendingAdds() {
        return pendingAddItems;
    }

    public List<Object> computers() {
        return computers;
    }

    public Map<Object, Set<Integer>> openPorts() {
        return openPorts;
    }

    public long lastMessage() {
        return lastMessage;
    }

    public void lastMessage(long value) {
        lastMessage = value;
    }

    private int _tickPackets = 0;
    private final int[] _packetHistory = new int[20];
    private int _packetHistoryIndex = 0;

    public int packetsPerCycleAvg() {
        int sum = 0;
        for (int v : _packetHistory) sum += v;
        return sum / _packetHistory.length;
    }

    @Override
    public void onSwitchActivity() {
        var now = System.currentTimeMillis();
        if (now - lastMessage >= (relayDelay - 1) * 50L) {
            super.onSwitchActivity();
            lastMessage = now;
            _tickPackets++;
            li.cil.oc.core.impl.common.PacketSender.sendSwitchActivity(this);
        }
    }

    @Override
    public void updateEntity() {
        if (isServer()) {
            _packetHistory[_packetHistoryIndex] = _tickPackets;
            _packetHistoryIndex = (_packetHistoryIndex + 1) % _packetHistory.length;
            _tickPackets = 0;
        }
        super.updateEntity();
    }
}
