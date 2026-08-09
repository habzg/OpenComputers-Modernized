package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.item.Analyzer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class AnalyzerEventHandler {
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        Player player = e.getEntity();
        ItemStack held = player.getItemInHand(e.getHand());
        var info = Items.get(held);
        if (info != null && info == Items.get(Constants.ItemName.Analyzer)) {
            if (Analyzer.analyze(e.getTarget(), player, Direction.DOWN, 0, 0, 0)) {
                player.swing(e.getHand());
                e.setCanceled(true);
            }
        }
    }
}
