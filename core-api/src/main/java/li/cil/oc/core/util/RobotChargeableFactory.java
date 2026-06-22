package li.cil.oc.core.util;

public abstract class RobotChargeableFactory {
    private static RobotChargeableFactory instance;

    public static void setInstance(RobotChargeableFactory inst) {
        instance = inst;
    }

    public static RobotChargeableFactory get() {
        return instance;
    }

    public abstract Object tryCreate(Object te);
}
