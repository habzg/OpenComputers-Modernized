package li.cil.oc.core.util;

public abstract class WaypointHelper {
    private static WaypointHelper instance;

    public static WaypointHelper get() {
        if (instance == null) {
            try {
                instance = (WaypointHelper) Class.forName("li.cil.oc.core.util.WaypointHelperImpl").getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
            }
        }
        return instance;
    }

    public abstract void add(Object waypoint);

    public abstract void remove(Object waypoint);
}
