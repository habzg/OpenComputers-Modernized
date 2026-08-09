package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;

public class UpgradeBattery extends AbstractManagedEnvironment implements DeviceInfo {
    @SuppressWarnings("unused")
    public final li.cil.oc.api.network.Node node;
    private final java.util.Map<String, String> deviceInfo;

    public UpgradeBattery(int tier) {
        this.node = Network.newNode(this, Visibility.Network)
                .withConnector(OCSettings.get().bufferCapacitorUpgrades[tier])
                .create();
        this.deviceInfo = java.util.Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Battery",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Unlimited Power (Almost Ed.)",
                DeviceAttribute.Capacity, String.valueOf(OCSettings.get().bufferCapacitorUpgrades[tier])
        );
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }
}
