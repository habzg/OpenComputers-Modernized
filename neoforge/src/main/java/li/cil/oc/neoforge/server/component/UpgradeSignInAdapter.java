package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.util.ExtendedArguments;
import org.jetbrains.annotations.NotNull;

public class UpgradeSignInAdapter extends UpgradeSign {
    public final EnvironmentHost host;

    @SuppressWarnings("unused")
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("sign", Visibility.Network)
            .withConnector()
            .create();

    public UpgradeSignInAdapter(EnvironmentHost host) {
        this.host = host;
    }

    @Override
    public @NotNull EnvironmentHost host() {
        return host;
    }

    @Callback(doc = "function(side:number):string -- Get the text on the sign on the specified side of the adapter.")
    public Object[] getValue(Context context, Arguments args) {
        return super.getValue(findSign(ExtendedArguments.checkSideAny(args, 0)));
    }

    @Callback(doc = "function(side:number, value:string):string -- Set the text on the sign on the specified side of the adapter.")
    public Object[] setValue(Context context, Arguments args) {
        return super.setValue(findSign(ExtendedArguments.checkSideAny(args, 0)), args.checkString(1));
    }
}
