package li.cil.oc.core.impl.integration.util;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import li.cil.oc.core.impl.common.ReflectionUtil;
import net.minecraft.world.item.ItemStack;

public final class ItemCharge {
    private static final Set<Charger> chargers = new LinkedHashSet<>();

    private ItemCharge() {
    }

    public static void add(Method canCharge, Method charge) {
        chargers.add(new Charger(canCharge, charge));
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            for (Charger charger : chargers) {
                if ((Boolean) ReflectionUtil.tryInvokeStatic(charger.canCharge, false, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static double charge(ItemStack stack, double amount) {
        if (stack != null && !stack.isEmpty()) {
            for (Charger charger : chargers) {
                if ((Boolean) ReflectionUtil.tryInvokeStatic(charger.canCharge, false, stack)) {
                    return (Double) ReflectionUtil.tryInvokeStatic(charger.charge, 0.0, stack, amount, false);
                }
            }
        }
        return amount;
    }

    private record Charger(Method canCharge, Method charge) {
    }
}
