package li.cil.oc.neoforge.integration.create;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerCreate {
    private EventHandlerCreate() {
    }

    public static boolean useWrench(Player player, int ignoredX, int ignoredY, int ignoredZ, boolean ignoredChangeDurability) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        return held.getItem() instanceof WrenchItem;
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof WrenchItem;
    }
}
