package li.cil.oc.core.impl.util;

public abstract class DriverScreenHelper {
    private static class Holder {
        static final DriverScreenHelper instance = new DriverScreenHelperImpl();
    }

    public static DriverScreenHelper get() {
        return Holder.instance;
    }

    public abstract boolean isDriverScreen(Object driver);

    public abstract void clearDataTag(Object driver, net.minecraft.world.item.ItemStack stack);
}
