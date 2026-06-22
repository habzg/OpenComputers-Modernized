package li.cil.oc.core.impl.integration.util;

import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Wrench {
    private static final Set<Method> usages = new LinkedHashSet<>();
    private static final Set<Method> checks = new LinkedHashSet<>();

    private Wrench() {
    }

    public static void addUsage(Method wrench) {
        usages.add(wrench);
    }

    public static void addCheck(Method checker) {
        checks.add(checker);
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            for (Method check : checks) {
                try {
                    if ((Boolean) check.invoke(null, stack)) return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    public static boolean holdsApplicableWrench(Player player, BlockPosition position) {
        if (!player.getMainHandItem().isEmpty()) {
            for (Method usage : usages) {
                try {
                    if ((Boolean) usage.invoke(null, player, position.x(), position.y(), position.z(), false))
                        return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    public static void wrenchUsed(Player player, BlockPosition position) {
        if (!player.getMainHandItem().isEmpty()) {
            for (Method usage : usages) {
                try {
                    usage.invoke(null, player, position.x(), position.y(), position.z(), true);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
