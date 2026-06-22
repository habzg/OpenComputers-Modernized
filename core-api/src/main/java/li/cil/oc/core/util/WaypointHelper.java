package li.cil.oc.core.util;

public abstract class WaypointHelper {
    private static WaypointHelper instance;

    public static void setInstance(WaypointHelper inst) {
        instance = inst;
    }

    public static WaypointHelper get() {
        return instance;
    }

    public abstract void add(Object waypoint);

    public abstract void remove(Object waypoint);
}
