package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;

public class Memory extends AbstractManagedEnvironment implements DeviceInfo {
    @SuppressWarnings("unused")
    public final Node node = Network.newNode(this, Visibility.Neighbors).create();
    private final Map<String, String> deviceInfo;

    public Memory(int tier) {
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Memory, DeviceAttribute.Description, "Memory bank", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Multipurpose RAM Type", DeviceAttribute.Clock, String.valueOf((int) (OCSettings.get().callBudgets[tier] * 1000)));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

}
