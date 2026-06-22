package li.cil.oc.core.impl.util;

import li.cil.oc.api.driver.Item;

import java.util.function.Predicate;

public final class ComponentDriverHelper {
    private static Predicate<Item> redstoneCardCheck = d -> false;

    private ComponentDriverHelper() {
    }

    public static void setRedstoneCardCheck(Predicate<Item> check) {
        redstoneCardCheck = check;
    }

    public static boolean isRedstoneCard(Item driver) {
        return redstoneCardCheck.test(driver);
    }
}
