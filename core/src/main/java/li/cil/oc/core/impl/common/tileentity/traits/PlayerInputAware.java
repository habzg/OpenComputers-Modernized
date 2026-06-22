package li.cil.oc.core.impl.common.tileentity.traits;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface PlayerInputAware {
    void onSetInventorySlotContents(Player player, int slot, ItemStack stack);
}
