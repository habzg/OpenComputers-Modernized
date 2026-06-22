package li.cil.oc.neoforge.integration.enderio;

import com.enderio.enderio.content.tools.YetaWrenchItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerEnderIO {
    private EventHandlerEnderIO() {
    }

    public static boolean useWrench(Player player, int ignoredX, int ignoredY, int ignoredZ, boolean ignoredChangeDurability) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        return held.getItem() instanceof YetaWrenchItem;
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof YetaWrenchItem;
    }
}
