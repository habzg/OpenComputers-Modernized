package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.TabletWrapper;
import li.cil.oc.core.util.ResultWrapper;

public class Tablet extends AbstractManagedEnvironment implements DeviceInfo {
    public final TabletWrapper tablet;
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("tablet")
            .withConnector(OCSettings.get().bufferTablet)
            .create();
    private final Map<String, String> deviceInfo;

    public Tablet(TabletWrapper tablet) {
        this.tablet = tablet;
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.System, DeviceAttribute.Description, "Tablet", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Jogger", DeviceAttribute.Capacity, String.valueOf(tablet.getContainerSize()));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(doc = "function():number -- Gets the pitch of the player holding the tablet.")
    public Object[] getPitch(Context context, Arguments args) {
        return ResultWrapper.result((double) tablet.player().getXRot());
    }

    @Callback(doc = "function():number -- Gets the yaw of the player holding the tablet.")
    public Object[] getYaw(Context context, Arguments args) {
        return ResultWrapper.result((double) tablet.player().getYRot());
    }
}
