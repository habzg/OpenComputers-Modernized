package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.Direction;

public interface SideRestricted {
    Direction checkSideForAction(Arguments args, int n);
}
