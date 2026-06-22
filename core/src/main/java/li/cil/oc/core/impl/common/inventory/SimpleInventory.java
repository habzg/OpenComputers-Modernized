package li.cil.oc.core.impl.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SimpleInventory extends Container {
    @Override
    default boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    default int getMaxStackSize() {
        return 64;
    }

    @SuppressWarnings("SameReturnValue")
    default int getInventoryStackRequired() {
        return 1;
    }

    @Override
    default void setChanged() {
    }

    @Override
    default @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < getContainerSize()) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                if (stack.getCount() - amount < getInventoryStackRequired()) {
                    setItem(slot, ItemStack.EMPTY);
                    return stack;
                } else {
                    ItemStack result = stack.split(amount);
                    setChanged();
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    default @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < getContainerSize()) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    default boolean stillValid(net.minecraft.world.entity.player.@NotNull Player player) {
        return true;
    }

    @Override
    default void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }
}
