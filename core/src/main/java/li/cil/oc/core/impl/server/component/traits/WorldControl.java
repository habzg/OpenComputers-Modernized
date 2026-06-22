package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;

public interface WorldControl extends WorldAware, SideRestricted {
    @Callback(doc = "function(side:number):boolean, string -- Checks the contents of the block on the specified sides and returns the findings.")
    default Object[] detect(Context context, Arguments args) {
        Direction side = checkSideForAction(args, 0);
        Object[] bc = blockContent(side);
        return ResultWrapper.result(bc[0], bc[1]);
    }
}
