package li.cil.oc.core.util;

import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.server.network.WaypointManager;

public class WaypointHelperImpl extends WaypointHelper {
    @Override
    public void add(Object waypoint) {
        WaypointManager.add((Waypoint) waypoint);
    }

    @Override
    public void remove(Object waypoint) {
        WaypointManager.remove((Waypoint) waypoint);
    }
}
