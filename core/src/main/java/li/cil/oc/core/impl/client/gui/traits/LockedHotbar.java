package li.cil.oc.core.impl.client.gui.traits;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface LockedHotbar {
    @SuppressWarnings("unused")
    int lockedSlot();

    @SuppressWarnings("unused")
    void setLockedSlot(int slot);

    default ItemStack lockedStack() {
        return null;
    }

    default boolean shouldSuppressClick(Slot slot) {
        ItemStack locked = lockedStack();
        return locked != null && !locked.isEmpty() && slot != null && slot.hasItem() && ItemStack.isSameItem(slot.getItem(), locked);
    }
}
