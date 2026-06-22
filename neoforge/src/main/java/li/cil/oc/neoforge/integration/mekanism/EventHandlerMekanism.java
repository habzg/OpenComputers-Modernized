package li.cil.oc.neoforge.integration.mekanism;

import mekanism.api.MekanismItemAbilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

@SuppressWarnings("unused")
public final class EventHandlerMekanism {
    private EventHandlerMekanism() {
    }

    public static boolean useWrench(Player player, int ignoredX, int ignoredY, int ignoredZ, boolean ignoredChangeDurability) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        return held.canPerformAction(MekanismItemAbilities.WRENCH_DISMANTLE);
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(Tags.Items.TOOLS_WRENCH);
    }
}
