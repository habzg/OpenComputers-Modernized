package li.cil.oc.fabric.common.inventory;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.inventory.ItemStackInventory;
import li.cil.oc.fabric.integration.opencomputers.DriverUpgradeDatabase;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

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
  default boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
    return !ItemStack.matches(stack, container());
  }
}
