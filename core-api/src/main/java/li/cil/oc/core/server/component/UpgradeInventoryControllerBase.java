package li.cil.oc.core.server.component;

import java.util.Map;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.core.Constants;

@SuppressWarnings("unused")
public class UpgradeInventoryControllerBase {
    public interface Common extends DeviceInfo {
        Map<String, String> INVENTORY_CONTROLLER_INFO = new java.util.HashMap<>() {{
            put(DeviceAttribute.Class, DeviceClass.Generic);
            put(DeviceAttribute.Description, "Inventory controller");
            put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            put(DeviceAttribute.Product, "Item Cataloguer R1");
        }};

        @Override
        default Map<String, String> getDeviceInfo() {
            return INVENTORY_CONTROLLER_INFO;
        }
    }
}
