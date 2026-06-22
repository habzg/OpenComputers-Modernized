package li.cil.oc.core.util;


public abstract class FluidTankHelper {
    private static FluidTankHelper instance;

    public static void setInstance(FluidTankHelper inst) {
        instance = inst;
    }

    public static FluidTankHelper get() {
        return instance;
    }

    public abstract Object createMultiTank(Object inv);
}
