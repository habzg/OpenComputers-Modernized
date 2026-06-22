package li.cil.oc.neoforge.integration.projectred;

import mrtjp.projectred.api.IScrewdriver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerProjectRed {
    public static boolean useWrench(Player player, int x, int y, int z, boolean changeDurability) {
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof IScrewdriver screwdriver) {
            if (changeDurability) {
                screwdriver.damageScrewdriver(player, InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof IScrewdriver;
    }
}
