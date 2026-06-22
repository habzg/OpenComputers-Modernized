package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.Map;


public abstract class WirelessNetworkCard extends NetworkCard implements WirelessEndpoint {
    private double strength;

    {
        strength = maxWirelessRange();
    }

    public WirelessNetworkCard(EnvironmentHost host) {
        super(host);
        this.node = Network.newNode(this, Visibility.Network)
                .withComponent("modem", Visibility.Neighbors)
                .withConnector()
                .create();
    }

    protected abstract double wirelessCostPerRange();

    protected abstract double maxWirelessRange();

    protected abstract boolean shouldSendWiredTraffic();

    @Override
    public int x() {
        return BlockPosition.apply(host).x();
    }

    @Override
    public int y() {
        return BlockPosition.apply(host).y();
    }

    @Override
    public int z() {
        return BlockPosition.apply(host).z();
    }

    @Override
    public Level level() {
        return host.level();
    }

    public void receivePacket(Packet packet, WirelessEndpoint source) {
        double dx = (source.x() + 0.5) - host.xPosition();
        double dy = (source.y() + 0.5) - host.yPosition();
        double dz = (source.z() + 0.5) - host.zPosition();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        receivePacket(packet, distance, host);
    }

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when sending messages.")
    public Object[] getStrength(Context context, Arguments args) {
        return ResultWrapper.result(strength);
    }

    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when sending messages.")
    public Object[] setStrength(Context context, Arguments args) {
        strength = Math.clamp(args.checkDouble(0), 0, maxWirelessRange());
        return ResultWrapper.result(strength);
    }

    @Override
    public Object[] isWireless(Context context, Arguments args) {
        return ResultWrapper.result(true);
    }

    @Override
    public Object[] isWired(Context context, Arguments args) {
        return ResultWrapper.result(shouldSendWiredTraffic());
    }

    @Override
    protected void doSend(Packet packet) {
        if (strength > 0) {
            checkPower();
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (shouldSendWiredTraffic())
            super.doSend(packet);
    }

    @Override
    protected void doBroadcast(Packet packet) {
        if (strength > 0) {
            checkPower();
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (shouldSendWiredTraffic())
            super.doBroadcast(packet);
    }

    private void checkPower() {
        double cost = wirelessCostPerRange();
        if (cost > 0 && !Settings.get().ignorePower) {
            if (!((li.cil.oc.api.network.Connector) node).tryChangeBuffer(-strength * cost)) {
                throw new RuntimeException(new IOException("not enough energy"));
            }
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (level().getGameTime() % 20 == 0) {
            Network.updateWirelessNetwork(this);
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            Network.joinWirelessNetwork(this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        var wnPos = new net.minecraft.core.BlockPos(x(), y(), z());
        if (node == this.node || !level().hasChunk(wnPos.getX() >> 4, wnPos.getZ() >> 4)) {
            Network.leaveWirelessNetwork(this);
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains("strength")) {
            strength = Math.clamp(nbt.getDouble("strength"), 0, maxWirelessRange());
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putDouble("strength", strength);
    }

    public static class Tier1 extends WirelessNetworkCard {
        private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
            put(DeviceAttribute.Class, DeviceClass.Network);
            put(DeviceAttribute.Description, "Wireless ethernet controller");
            put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            put(DeviceAttribute.Product, "39i110 (LPPW-01)");
            put(DeviceAttribute.Version, "1.0");
            put(DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize));
            put(DeviceAttribute.Size, String.valueOf(maxOpenPorts()));
            put(DeviceAttribute.Width, String.valueOf(maxWirelessRange()));
        }};

        public Tier1(EnvironmentHost host) {
            super(host);
        }

        @Override
        protected double wirelessCostPerRange() {
            return Settings.get().wirelessCostPerRange[Tier.One];
        }

        @Override
        protected double maxWirelessRange() {
            return Settings.get().maxWirelessRange[Tier.One];
        }

        @Override
        protected int maxOpenPorts() {
            return Settings.get().maxOpenPorts[Tier.One + 1];
        }

        @Override
        protected boolean shouldSendWiredTraffic() {
            return false;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        @Override
        public boolean isPacketAccepted(Packet packet, double distance) {
            if (distance <= maxWirelessRange() && (distance > 0 || shouldSendWiredTraffic())) {
                return super.isPacketAccepted(packet, distance);
            }
            return false;
        }
    }

    public static class Tier2 extends Tier1 {
        private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
            put(DeviceAttribute.Class, DeviceClass.Network);
            put(DeviceAttribute.Description, "Wireless ethernet controller");
            put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            put(DeviceAttribute.Product, "62i230 (MPW-01)");
            put(DeviceAttribute.Version, "2.0");
            put(DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize));
            put(DeviceAttribute.Size, String.valueOf(maxOpenPorts()));
            put(DeviceAttribute.Width, String.valueOf(maxWirelessRange()));
        }};

        public Tier2(EnvironmentHost host) {
            super(host);
        }

        @Override
        protected double wirelessCostPerRange() {
            return Settings.get().wirelessCostPerRange[Tier.Two];
        }

        @Override
        protected double maxWirelessRange() {
            return Settings.get().maxWirelessRange[Tier.Two];
        }

        @Override
        protected int maxOpenPorts() {
            return Settings.get().maxOpenPorts[Tier.Two + 1];
        }

        @Override
        protected boolean shouldSendWiredTraffic() {
            return true;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }
    }
}
