package li.cil.oc.core.impl.common;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public final class ToolDurabilityProviders {
    private static final org.slf4j.Logger LOGGER = getLogger(ToolDurabilityProviders.class);
    private static final List<Method> providers = new ArrayList<>();

    private ToolDurabilityProviders() {
    }

    public static void add(Method provider) {
        providers.add(provider);
    }

    public static Double getDurability(ItemStack stack) {
        for (Method provider : providers) {
            try {
                Object result = provider.invoke(null, Double.NaN, stack);
                if (result instanceof Double durability && !durability.isNaN()) return durability;
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("Error invoking durability provider", e);
            }
        }
        if (stack.isDamageableItem()) {
            return 1.0 - (double) stack.getDamageValue() / (double) stack.getMaxDamage();
        }
        return null;
    }

}
