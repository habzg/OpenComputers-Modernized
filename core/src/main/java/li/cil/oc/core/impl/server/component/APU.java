package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;

public class APU extends GraphicsCardBase {
    private final Map<String, String> deviceInfo;

    public APU(int tier) {
        super(tier);
        this.deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Processor, DeviceAttribute.Description, "APU", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "FlexiArch " + (tier + 1) + " Processor (Builtin Graphics)", DeviceAttribute.Capacity, capacityInfo(), DeviceAttribute.Width, widthInfo(), DeviceAttribute.Clock, (OCSettings.get().callBudgets[tier] * 1000) + "+" + clockInfo());
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

}
