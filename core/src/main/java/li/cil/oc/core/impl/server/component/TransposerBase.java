package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.ExtendedArguments;
import net.minecraft.core.Direction;


import java.util.Map;

public abstract class TransposerBase extends ManagedEnvironment implements DeviceInfo {
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("transposer")
            .withConnector()
            .create();
    private final Map<String, String> deviceInfo;

    public TransposerBase() {
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Generic, DeviceAttribute.Description, "Transposer", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "TP4k-iX");
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @SuppressWarnings("unused")
    public Direction checkSideForAction(Arguments args, int n) {
        return ExtendedArguments.checkSideAny(args, n);
    }

    public String onTransferContents() {
        if (((Connector) node).tryChangeBuffer(-Settings.get().transposerCost)) return null;
        return "not enough energy";
    }
}
