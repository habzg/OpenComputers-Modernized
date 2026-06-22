package li.cil.oc.core.impl.util;

import net.minecraft.core.Direction;

public abstract class GeolyzerHostHelper {
    private static GeolyzerHostHelper instance;

    public static void setInstance(GeolyzerHostHelper inst) {
        instance = inst;
    }

    public static GeolyzerHostHelper get() {
        return instance;
    }

    public abstract boolean isRobot(Object host);

    public abstract BlockPosition robotPosition(Object host);

    public abstract Direction robotToGlobal(Object host, Direction side);
}
