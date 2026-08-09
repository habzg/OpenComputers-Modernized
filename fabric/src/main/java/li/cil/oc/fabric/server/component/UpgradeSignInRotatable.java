package li.cil.oc.fabric.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import org.jetbrains.annotations.NotNull;

public class UpgradeSignInRotatable extends UpgradeSign {
    public final EnvironmentHost host;

    @SuppressWarnings("unused")
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("sign", Visibility.Neighbors)
            .withConnector()
            .create();

    @SuppressWarnings("unused")
    public UpgradeSignInRotatable(EnvironmentHost host) {
        this.host = host;
    }

    @Override
    public @NotNull EnvironmentHost host() {
        return host;
    }

    @Callback(doc = "function():string -- Get the text on the sign in front of the host.")
    public Object[] getValue(Context context, Arguments args) {
        return super.getValue(findSign(((Rotatable) host).facing()));
    }

    @Callback(doc = "function(value:string):string -- Set the text on the sign in front of the host.")
    public Object[] setValue(Context context, Arguments args) {
        return super.setValue(findSign(((Rotatable) host).facing()), args.checkString(0));
    }
}
