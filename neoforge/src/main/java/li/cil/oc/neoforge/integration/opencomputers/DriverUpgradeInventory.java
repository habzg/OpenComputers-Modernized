package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Inventory;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradeInventory extends Item implements Inventory, HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.InventoryUpgrade));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int inventoryCapacity(ItemStack stack) {
        return 16;
    }
}
