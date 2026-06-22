package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.common.tileentity.Waypoint;
import li.cil.oc.core.util.WaypointHelper;
import li.cil.oc.neoforge.server.network.Waypoints;

public class NeoWaypointHelper extends WaypointHelper {
    @Override
    public void add(Object waypoint) {
        Waypoints.add((Waypoint) waypoint);
    }

    @Override
    public void remove(Object waypoint) {
        Waypoints.remove((Waypoint) waypoint);
    }
}
