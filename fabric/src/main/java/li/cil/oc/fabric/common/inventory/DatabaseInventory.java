package li.cil.oc.fabric.common.inventory;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.inventory.ItemStackInventory;
import li.cil.oc.fabric.integration.opencomputers.DriverUpgradeDatabase;
import net.minecraft.world.item.ItemStack;

public interface DatabaseInventory extends ItemStackInventory {
    default int tier() {
        return new DriverUpgradeDatabase().tier(container());
    }

    @Override
    default int getContainerSize() {
        return OCSettings.get().databaseEntriesPerTier[tier()];
    }

    @Override
    default String inventoryName() {
        return "Database";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() != container().getItem();
    }
}
