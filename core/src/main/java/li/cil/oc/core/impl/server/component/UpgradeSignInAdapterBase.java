package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.util.ExtendedArguments;
import net.minecraft.core.Direction;

public abstract class UpgradeSignInAdapterBase extends UpgradeSignBase {
    @Callback(doc = "function(side:number):string -- Get the text on the sign on the specified side of the adapter.")
    public Object[] getValue(Context context, Arguments args) {
        return super.getValue(findSign(ExtendedArguments.checkSideAny(args, 0)));
    }

    @Callback(doc = "function(side:number, value:string):string -- Set the text on the sign on the specified side of the adapter.")
    public Object[] setValue(Context context, Arguments args) {
        Direction side = ExtendedArguments.checkSideAny(args, 0);
        return setValue(findSign(side), args.checkString(1));
    }
}
