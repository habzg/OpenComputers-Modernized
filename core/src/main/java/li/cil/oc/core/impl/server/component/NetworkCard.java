package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.server.component.traits.WakeMessageAware;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.core.util.ServerNetwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NetworkCard extends li.cil.oc.api.prefab.ManagedEnvironment implements RackBusConnectable, DeviceInfo, WakeMessageAware {
    public final EnvironmentHost host;
    protected final Visibility visibility;
    protected final Set<Integer> openPorts = new HashSet<>();
    public Node node;
    private String wakeMessage = null;
    private boolean wakeMessageFuzzy = false;

    public NetworkCard(EnvironmentHost host) {
        this.host = host;
        this.visibility = host instanceof Rack ? Visibility.Neighbors : Visibility.Network;
        this.node = Network.newNode(this, visibility)
                .withComponent("modem", Visibility.Neighbors)
                .create();
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(DeviceAttribute.Class, DeviceClass.Network, DeviceAttribute.Description, "Ethernet controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "42i520 (MPN-01)", DeviceAttribute.Version, "1.0", DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize), DeviceAttribute.Size, String.valueOf(maxOpenPorts()), DeviceAttribute.Width, String.valueOf(Settings.get().maxNetworkPacketParts));
    }

    @Override
    public String getWakeMessage() {
        return wakeMessage;
    }

    @Override
    public void setWakeMessage(String message) {
        this.wakeMessage = message;
    }

    @Override
    public boolean isWakeMessageFuzzy() {
        return wakeMessageFuzzy;
    }

    @Override
    public void setWakeMessageFuzzy(boolean fuzzy) {
        this.wakeMessageFuzzy = fuzzy;
    }

    protected int maxOpenPorts() {
        return Settings.get().maxOpenPorts[Tier.One];
    }

    @Callback(doc = "function(port:number):boolean -- Opens the specified port.")
    public Object[] open(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) return ResultWrapper.result(false);
        if (openPorts.size() >= maxOpenPorts())
            throw new RuntimeException(new java.io.IOException("too many open ports"));
        return ResultWrapper.result(openPorts.add(port));
    }

    @Callback(doc = "function([port:number]):boolean -- Closes the specified port.")
    public Object[] close(Context context, Arguments args) {
        if (args.count() == 0) {
            boolean closed = !openPorts.isEmpty();
            openPorts.clear();
            return ResultWrapper.result(closed);
        } else {
            int port = checkPort(args.checkInteger(0));
            return ResultWrapper.result(openPorts.remove(port));
        }
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Whether the specified port is open.")
    public Object[] isOpen(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        return ResultWrapper.result(openPorts.contains(port));
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wireless networking capability.")
    public Object[] isWireless(Context context, Arguments args) {
        return ResultWrapper.result(false);
    }

    @Callback(direct = true, doc = "function():boolean -- Whether this card has wired networking capability.")
    public Object[] isWired(Context context, Arguments args) {
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(address:string, port:number, data...) -- Sends the specified data.")
    public Object[] send(Context context, Arguments args) {
        String address = args.checkString(0);
        int port = checkPort(args.checkInteger(1));
        Object[] data = new Object[args.count() - 2];
        for (int i = 2; i < args.count(); i++) data[i - 2] = args.checkAny(i);
        Packet packet = Network.newPacket(node.address(), address, port, data);
        doSend(packet);
        networkActivity();
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(port:number, data...) -- Broadcasts the specified data on the specified port.")
    public Object[] broadcast(Context context, Arguments args) {
        int port = checkPort(args.checkInteger(0));
        Object[] data = new Object[args.count() - 1];
        for (int i = 1; i < args.count(); i++) data[i - 1] = args.checkAny(i);
        Packet packet = Network.newPacket(node.address(), null, port, data);
        doBroadcast(packet);
        networkActivity();
        return ResultWrapper.result(true);
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size.")
    public Object[] maxPacketSize(Context context, Arguments args) {
        return ResultWrapper.result((double) Settings.get().maxNetworkPacketSize);
    }

    protected void doSend(Packet packet) {
        if (visibility == Visibility.Neighbors) node.sendToNeighbors("network.message", packet);
        else if (visibility == Visibility.Network) node.sendToReachable("network.message", packet);
    }

    protected void doBroadcast(Packet packet) {
        if (visibility == Visibility.Neighbors) node.sendToNeighbors("network.message", packet);
        else if (visibility == Visibility.Network) node.sendToReachable("network.message", packet);
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            openPorts.clear();
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) && node.isNeighborOf(message.source())) {
            openPorts.clear();
        }
        if ("network.message".equals(message.name()) && message.data().length > 0 && message.data()[0] instanceof Packet) {
            receivePacket((Packet) message.data()[0]);
        }
    }

    @Override
    public boolean isPacketAccepted(Packet packet, double distance) {
        if (openPorts.contains(packet.port())) {
            networkActivity();
            return true;
        }
        return false;
    }

    @Override
    public void receivePacket(Packet packet) {
        receivePacket(packet, 0, host);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        openPorts.clear();
        for (int p : nbt.getIntArray("openPorts")) openPorts.add(p);
        loadWakeMessage(nbt);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        int[] ports = openPorts.stream().mapToInt(Integer::intValue).toArray();
        nbt.putIntArray("openPorts", ports);
        saveWakeMessage(nbt);
    }

    protected int checkPort(int port) {
        if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number");
        return port;
    }

    private void networkActivity() {
        if (host != null) {
            ServerNetwork.get().sendNetworkActivity(node, host);
        }
    }
}
