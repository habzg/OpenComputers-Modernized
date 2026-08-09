package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.blockentity.traits.BundledRedstoneAware;
import li.cil.oc.core.impl.integration.util.WirelessRedstone;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class Redstone {
    private Redstone() {
    }

    public static class Vanilla extends RedstoneVanilla {
        private final EnvironmentHost host;

        public Vanilla(EnvironmentHost host) {
            this.host = host;
        }

        @Override
        public EnvironmentHost redstone() {
            return host;
        }

        @Override
        public Node node() {
            return super.node;
        }

    }

    public static class Bundled extends RedstoneVanilla implements RedstoneBundled {
        private final EnvironmentHost host;
        private final Map<String, String> deviceInfo;

        public Bundled(EnvironmentHost host) {
            this.host = host;
            deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Communication, DeviceAttribute.Description, "Advanced redstone controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Rb800-M", DeviceAttribute.Capacity, "65536", DeviceAttribute.Width, "16");
        }

        @Override
        public EnvironmentHost redstone() {
            return host;
        }

        @Override
        public BundledRedstoneAware bundledRedstone() {
            return (BundledRedstoneAware) host;
        }

        @Override
        public Node node() {
            return super.node;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }
    }

    public static class Wireless extends RedstoneSignaller implements RedstoneWireless {
        private final EnvironmentHost host;
        private final Map<String, String> deviceInfo;
        private boolean wirelessOutput = false;
        private int wirelessFrequency = 0;

        private boolean wirelessInput = false;

        public Wireless(EnvironmentHost host) {
            this.host = host;
            deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Communication, DeviceAttribute.Description, "Wireless redstone controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Rw400-M", DeviceAttribute.Capacity, "1", DeviceAttribute.Width, "1");
        }

        @Override
        public EnvironmentHost redstone() {
            return host;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        @Override
        public boolean getWirelessOutputValue() {
            return wirelessOutput;
        }

        @Override
        public void setWirelessOutputValue(boolean value) {
            wirelessOutput = value;
        }

        @Override
        public boolean getWirelessInputValue() {
            return wirelessInput;
        }

        @Override
        public void setWirelessInputValue(boolean value) {
            wirelessInput = value;
            host.markChanged();
        }

        @Override
        public int getWirelessFrequencyValue() {
            return wirelessFrequency;
        }

        @Override
        public void setWirelessFrequencyValue(int value) {
            wirelessFrequency = value;
            host.markChanged();
        }

        @Override
        public int getFreq() {
            return wirelessFrequency;
        }

        @Override
        public void onConnect(Node node) {
            super.onConnect(node);
            if (node == this.node) {
                WirelessRedstone.addReceiver(this);
            }
        }

        @Override
        public void onDisconnect(Node node) {
            super.onDisconnect(node);
            if (node == this.node) {
                WirelessRedstone.removeReceiver(this);
                WirelessRedstone.removeTransmitter(this);
                wirelessOutput = false;
                wirelessFrequency = 0;
            }
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            wirelessFrequency = nbt.getInt("wirelessFrequency");
            wirelessInput = nbt.getBoolean("wirelessInput");
            wirelessOutput = nbt.getBoolean("wirelessOutput");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putInt("wirelessFrequency", wirelessFrequency);
            nbt.putBoolean("wirelessInput", wirelessInput);
            nbt.putBoolean("wirelessOutput", wirelessOutput);
        }
    }

    public static class VanillaWireless extends RedstoneVanilla implements RedstoneWireless {
        private final EnvironmentHost host;
        private boolean wirelessOutput = false;
        private int wirelessFrequency = 0;

        private boolean wirelessInput = false;

        public VanillaWireless(EnvironmentHost host) {
            this.host = host;
        }

        @Override
        public EnvironmentHost redstone() {
            return host;
        }

        @Override
        public boolean getWirelessOutputValue() {
            return wirelessOutput;
        }

        @Override
        public void setWirelessOutputValue(boolean value) {
            wirelessOutput = value;
        }

        @Override
        public boolean getWirelessInputValue() {
            return wirelessInput;
        }

        @Override
        public void setWirelessInputValue(boolean value) {
            wirelessInput = value;
            host.markChanged();
        }

        @Override
        public int getWirelessFrequencyValue() {
            return wirelessFrequency;
        }

        @Override
        public void setWirelessFrequencyValue(int value) {
            wirelessFrequency = value;
            host.markChanged();
        }

        @Override
        public int getFreq() {
            return wirelessFrequency;
        }

        @Override
        public void onConnect(Node node) {
            super.onConnect(node);
            if (node == this.node) {
                WirelessRedstone.addReceiver(this);
            }
        }

        @Override
        public void onDisconnect(Node node) {
            super.onDisconnect(node);
            if (node == this.node) {
                WirelessRedstone.removeReceiver(this);
                WirelessRedstone.removeTransmitter(this);
                wirelessOutput = false;
                wirelessFrequency = 0;
            }
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            wirelessFrequency = nbt.getInt("wirelessFrequency");
            wirelessInput = nbt.getBoolean("wirelessInput");
            wirelessOutput = nbt.getBoolean("wirelessOutput");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putInt("wirelessFrequency", wirelessFrequency);
            nbt.putBoolean("wirelessInput", wirelessInput);
            nbt.putBoolean("wirelessOutput", wirelessOutput);
        }
    }

    public static class BundledWireless extends RedstoneVanilla implements RedstoneBundled, RedstoneWireless {
        private final EnvironmentHost host;
        private final Map<String, String> deviceInfo;
        private boolean wirelessOutput = false;
        private int wirelessFrequency = 0;

        private boolean wirelessInput = false;

        public BundledWireless(EnvironmentHost host) {
            this.host = host;
            deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Communication, DeviceAttribute.Description, "Combined redstone controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Rx900-M", DeviceAttribute.Capacity, "65536", DeviceAttribute.Width, "16");
        }

        @Override
        public EnvironmentHost redstone() {
            return host;
        }

        @Override
        public BundledRedstoneAware bundledRedstone() {
            return (BundledRedstoneAware) host;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        @Override
        public boolean getWirelessOutputValue() {
            return wirelessOutput;
        }

        @Override
        public void setWirelessOutputValue(boolean value) {
            wirelessOutput = value;
        }

        @Override
        public boolean getWirelessInputValue() {
            return wirelessInput;
        }

        @Override
        public void setWirelessInputValue(boolean value) {
            wirelessInput = value;
            host.markChanged();
        }

        @Override
        public int getWirelessFrequencyValue() {
            return wirelessFrequency;
        }

        @Override
        public void setWirelessFrequencyValue(int value) {
            wirelessFrequency = value;
            host.markChanged();
        }

        @Override
        public int getFreq() {
            return wirelessFrequency;
        }

        @Override
        public void onConnect(Node node) {
            super.onConnect(node);
            if (node == this.node) {
                WirelessRedstone.addReceiver(this);
            }
        }

        @Override
        public void onDisconnect(Node node) {
            super.onDisconnect(node);
            if (node == this.node) {
                WirelessRedstone.removeReceiver(this);
                WirelessRedstone.removeTransmitter(this);
                wirelessOutput = false;
                wirelessFrequency = 0;
            }
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            wirelessFrequency = nbt.getInt("wirelessFrequency");
            wirelessInput = nbt.getBoolean("wirelessInput");
            wirelessOutput = nbt.getBoolean("wirelessOutput");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putInt("wirelessFrequency", wirelessFrequency);
            nbt.putBoolean("wirelessInput", wirelessInput);
            nbt.putBoolean("wirelessOutput", wirelessOutput);
        }
    }
}
