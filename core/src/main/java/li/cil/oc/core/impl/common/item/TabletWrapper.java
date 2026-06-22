package li.cil.oc.core.impl.common.item;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface TabletWrapper extends Container {
    @SuppressWarnings("SameReturnValue")
    String containerSlotType();

    int containerSlotTier();

    Player player();

    ItemStack getStack();
}
