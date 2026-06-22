package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;

import java.util.Map;

public class CPU extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    @SuppressWarnings("unused")
    public final Node node;
    private final Map<String, String> deviceInfo;

    public CPU(int tier) {
        this.node = Network.newNode(this, Visibility.Neighbors).create();
        this.deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Processor, DeviceAttribute.Description, "CPU", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "FlexiArch " + (tier + 1) + " Processor", DeviceAttribute.Clock, String.valueOf((int) (Settings.get().callBudgets[tier] * 1000)));
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

}
