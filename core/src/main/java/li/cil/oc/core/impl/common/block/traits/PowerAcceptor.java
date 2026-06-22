package li.cil.oc.core.impl.common.block.traits;

import li.cil.oc.core.impl.common.block.AbstractBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface PowerAcceptor {
    double energyThroughput();

    @SuppressWarnings("unused")
    default void tooltipTail(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced, AbstractBlock self) {
        self.tooltipTail(metadata, stack, player, tooltip, advanced);
    }
}
