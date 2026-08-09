package li.cil.oc.core.impl.common.inventory;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.impl.util.ItemUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ServerInventory extends ItemStackInventory {
    default int tier() {
        return Math.max(0, caseTier(container()));
    }

    private static int caseTier(ItemStack stack) {
      return ItemUtils.caseTier(stack);
    }

    @Override
    default int getContainerSize() {
        return InventorySlots.server[tier()].length;
    }

    @Override
    default String inventoryName() {
        return "Server";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    default boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= InventorySlots.server[tier()].length) return false;
        java.util.function.Supplier<Boolean> supplier = () -> {
            Object driver = li.cil.oc.api.API.driver.driverFor(stack, li.cil.oc.api.internal.Server.class);
            if (driver instanceof DriverItem itemDriver) {
                li.cil.oc.core.common.InventorySlots.InventorySlot provided = InventorySlots.server[tier()][slot];
                return itemDriver.slot(stack).equals(provided.slot()) && itemDriver.tier(stack) <= provided.tier();
            }
            return false;
        };
        return supplier.get();
    }
}
