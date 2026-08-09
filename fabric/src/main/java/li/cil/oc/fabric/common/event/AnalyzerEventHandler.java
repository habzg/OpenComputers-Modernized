package li.cil.oc.fabric.common.event;

import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.item.Analyzer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;

public class AnalyzerEventHandler {
    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            var held = player.getItemInHand(hand);
            var info = Items.get(held);
            if (info != null && info == Items.get(Constants.ItemName.Analyzer)) {
                if (Analyzer.analyze(entity, player, Direction.DOWN, 0, 0, 0)) {
                    player.swing(hand);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
    }
}
