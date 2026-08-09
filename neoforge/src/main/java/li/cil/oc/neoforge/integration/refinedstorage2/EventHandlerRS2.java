package li.cil.oc.neoforge.integration.refinedstorage2;

import com.refinedmods.refinedstorage.common.content.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerRS2 {
    private EventHandlerRS2() {
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
        return stack.is(Items.INSTANCE.getWrench());
    }
}