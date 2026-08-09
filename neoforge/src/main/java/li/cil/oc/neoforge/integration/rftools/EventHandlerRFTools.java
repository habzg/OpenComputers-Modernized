package li.cil.oc.neoforge.integration.rftools;

import mcjty.lib.api.smartwrench.SmartWrench;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerRFTools {
    private EventHandlerRFTools() {
    }

    public static boolean useWrench(Player player, int ignoredX, int ignoredY, int ignoredZ, boolean ignoredChangeDurability) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        return isWrench(held);
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof SmartWrench;
    }
}
