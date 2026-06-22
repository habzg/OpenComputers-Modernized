package li.cil.oc.core.server.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.core.Constants;

import java.util.Map;

@SuppressWarnings("unused")
public class UpgradeTankControllerBase {
    public interface Common extends DeviceInfo {
        Map<String, String> TANK_CONTROLLER_INFO = new java.util.HashMap<>() {{
            put(DeviceAttribute.Class, DeviceClass.Generic);
            put(DeviceAttribute.Description, "Tank controller");
            put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            put(DeviceAttribute.Product, "FlowCheckDX");
        }};

        @Override
        default Map<String, String> getDeviceInfo() {
            return TANK_CONTROLLER_INFO;
        }
    }
}
