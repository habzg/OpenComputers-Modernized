package li.cil.oc.core.impl.util;

public abstract class DriverScreenHelper {
    private static DriverScreenHelper instance;

    public static void setInstance(DriverScreenHelper inst) {
        instance = inst;
    }

    public static DriverScreenHelper get() {
        return instance;
    }

    public abstract boolean isDriverScreen(Object driver);

    public abstract void clearDataTag(Object driver, net.minecraft.world.item.ItemStack stack);
}
