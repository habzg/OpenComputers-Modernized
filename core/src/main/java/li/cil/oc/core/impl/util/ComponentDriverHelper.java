package li.cil.oc.core.impl.util;

import java.util.function.Predicate;
import li.cil.oc.api.driver.DriverItem;

public final class ComponentDriverHelper {
    private static Predicate<DriverItem> redstoneCardCheck = d -> false;

    private ComponentDriverHelper() {
    }

    public static void setRedstoneCardCheck(Predicate<DriverItem> check) {
        redstoneCardCheck = check;
    }

    public static boolean isRedstoneCard(DriverItem driver) {
        return redstoneCardCheck.test(driver);
    }
}
