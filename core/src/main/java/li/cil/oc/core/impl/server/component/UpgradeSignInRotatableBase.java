package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

public abstract class UpgradeSignInRotatableBase extends UpgradeSignBase {
    @Callback(doc = "function():string -- Get the text on the sign in front of the host.")
    public Object[] getValue(Context context, Arguments args) {
        return super.getValue(findSign(((Rotatable) host()).facing()));
    }

    @Callback(doc = "function(value:string):string -- Set the text on the sign in front of the host.")
    public Object[] setValue(Context context, Arguments args) {
        return setValue(findSign(((Rotatable) host()).facing()), args.checkString(0));
    }
}
