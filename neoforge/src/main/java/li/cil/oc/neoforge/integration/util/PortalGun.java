package li.cil.oc.neoforge.integration.util;

import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.world.item.ItemStack;

public final class PortalGun {
    private static Class<?> portalGunClass;

    static {
        try {
            portalGunClass = Class.forName("portalgun.common.item.ItemPortalGun");
        } catch (Throwable ignored) {
        }
    }

    private PortalGun() {
    }

    public static boolean isPortalGun(ItemStack stack) {
        return stack != null && !stack.isEmpty() &&
                Mods.PortalGun.isAvailable() &&
                portalGunClass != null &&
                portalGunClass.isAssignableFrom(stack.getItem().getClass());
    }

    public static boolean isStandardPortalGun(ItemStack stack) {
        return isPortalGun(stack) && stack.getDamageValue() == 0;
    }
}
