package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;


import java.util.Map;

public class UpgradeRITEG extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withConnector()
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Power);
        put(DeviceAttribute.Description, "Radioisotope thermoelectric generator");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Hazmat protection not included");
    }};

    public UpgradeRITEG(EnvironmentHost ignoredHost) {
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (node != null) {
            ((li.cil.oc.api.network.Connector) node).changeBuffer(Settings.get().ritegUpgradeEfficiency);
        }
    }
}
