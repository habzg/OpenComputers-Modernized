package li.cil.oc.neoforge.integration.appeng;

import appeng.datagen.providers.tags.ConventionTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerAE2 {
    private EventHandlerAE2() {
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
        return stack.is(ConventionTags.QUARTZ_WRENCH);
    }
}
