package li.cil.oc.core.impl.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface InventoryProxy extends Container {
    Container inventory();

    default int offset() {
        return 0;
    }

    @Override
    default int getContainerSize() {
        return inventory().getContainerSize();
    }

    @Override
    default int getMaxStackSize() {
        return inventory().getMaxStackSize();
    }

    @Override
    default boolean isEmpty() {
        return inventory().isEmpty();
    }

    @Override
    default boolean stillValid(@NotNull Player player) {
        return inventory().stillValid(player);
    }

    @Override
    default boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        int offsetSlot = slot + offset();
        return isValidSlot(offsetSlot) && inventory().canPlaceItem(offsetSlot, stack);
    }

    @Override
    default @NotNull ItemStack getItem(int slot) {
        int offsetSlot = slot + offset();
        if (isValidSlot(offsetSlot)) return inventory().getItem(offsetSlot);
        return ItemStack.EMPTY;
    }

    @Override
    default @NotNull ItemStack removeItem(int slot, int amount) {
        int offsetSlot = slot + offset();
        if (isValidSlot(offsetSlot)) return inventory().removeItem(offsetSlot, amount);
        return ItemStack.EMPTY;
    }

    @Override
    default @NotNull ItemStack removeItemNoUpdate(int slot) {
        int offsetSlot = slot + offset();
        if (isValidSlot(offsetSlot)) return inventory().removeItemNoUpdate(offsetSlot);
        return ItemStack.EMPTY;
    }

    @Override
    default void setItem(int slot, @NotNull ItemStack stack) {
        int offsetSlot = slot + offset();
        if (isValidSlot(offsetSlot)) inventory().setItem(offsetSlot, stack);
    }

    @Override
    default void setChanged() {
        inventory().setChanged();
    }

    @Override
    default void startOpen(@NotNull Player player) {
        inventory().startOpen(player);
    }

    @Override
    default void stopOpen(@NotNull Player player) {
        inventory().stopOpen(player);
    }

    default boolean isValidSlot(int slot) {
        return slot >= offset() && slot < getContainerSize() + offset();
    }
}
