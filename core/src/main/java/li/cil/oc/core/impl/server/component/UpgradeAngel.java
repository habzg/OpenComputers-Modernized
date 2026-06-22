package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;

import java.util.Map;

public class UpgradeAngel extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    @SuppressWarnings("unused")
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Angel upgrade");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "FreePlacer (TM)");
        put(DeviceAttribute.Capacity, String.valueOf(Settings.get().maxNetworkPacketSize));
    }};

    public UpgradeAngel() {
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }
}
