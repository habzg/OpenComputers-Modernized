package li.cil.oc.core.impl.server.component;

import java.util.Collection;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.traits.WakeMessageAware;
import li.cil.oc.core.server.network.QuantumNetwork;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class LinkedCard extends AbstractManagedEnvironment implements QuantumNetwork.QuantumNode, DeviceInfo, WakeMessageAware {
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("tunnel", Visibility.Neighbors)
            .withConnector()
            .create();
    private final Map<String, String> deviceInfo;
    public String tunnel = "creative";
    private String wakeMessage = null;
    private boolean wakeMessageFuzzy = false;

    public LinkedCard() {
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Network, DeviceAttribute.Description, "Quantumnet controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "HyperLink IV: Ender Edition", DeviceAttribute.Capacity, String.valueOf(OCSettings.get().maxNetworkPacketSize), DeviceAttribute.Width, String.valueOf(OCSettings.get().maxNetworkPacketParts));
    }

    @Override
    public String tunnel() {
        return tunnel;
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

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(doc = "function(data...) -- Sends the specified data to the card this one is linked to.")
    public Object[] send(Context context, Arguments args) {
        Collection<QuantumNetwork.QuantumNode> endpoints = QuantumNetwork.getEndpoints(tunnel);
        endpoints.remove(this);
        Object[] data = new Object[args.count()];
        for (int i = 0; i < args.count(); i++) data[i] = args.checkAny(i);
        Packet packet = Network.newPacket(node.address(), null, 0, data);
        if (((Connector) node).tryChangeBuffer(-(packet.size() / 32.0 + OCSettings.get().wirelessCostPerRange[Tier.Two] * OCSettings.get().maxWirelessRange[Tier.Two] * 5))) {
            for (QuantumNetwork.QuantumNode endpoint : endpoints) {
                endpoint.receivePacket(packet);
            }
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(null, "not enough energy");
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size (config setting).")
    public Object[] maxPacketSize(Context context, Arguments args) {
        return ResultWrapper.result((double) OCSettings.get().maxNetworkPacketSize);
    }

    public void receivePacket(Packet packet) {
        receivePacket(packet, 0, null);
    }

    @Callback(direct = true, doc = "function():string -- Gets this link card's shared channel address")
    public Object[] getChannel(Context context, Arguments args) {
        return ResultWrapper.result(tunnel);
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            QuantumNetwork.add(this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            QuantumNetwork.remove(this);
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains(OCSettings.namespace + "tunnel")) {
            tunnel = nbt.getString(OCSettings.namespace + "tunnel");
        }
        loadWakeMessage(nbt);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putString(OCSettings.namespace + "tunnel", tunnel);
        saveWakeMessage(nbt);
    }
}
