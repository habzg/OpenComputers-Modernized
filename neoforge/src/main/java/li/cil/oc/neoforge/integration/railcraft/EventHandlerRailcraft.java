package li.cil.oc.neoforge.integration.railcraft;

import mods.railcraft.api.item.Crowbar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class EventHandlerRailcraft {
    public static boolean useWrench(Player player, int x, int y, int z, boolean changeDurability) {
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof Crowbar crowbar) {
            var pos = new BlockPos(x, y, z);
            if (crowbar.canWhack(player, InteractionHand.MAIN_HAND, stack, pos)) {
                if (changeDurability && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    crowbar.onWhack(serverPlayer, InteractionHand.MAIN_HAND, stack, pos);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof Crowbar;
    }
}

