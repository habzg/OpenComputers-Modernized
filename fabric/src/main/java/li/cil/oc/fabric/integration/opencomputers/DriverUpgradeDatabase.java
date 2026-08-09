package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.fabric.common.inventory.DatabaseInventory;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradeDatabase extends Item implements li.cil.oc.api.driver.item.HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.DatabaseUpgradeTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.DatabaseUpgradeTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.DatabaseUpgradeTier3));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        var provider = host.level().registryAccess();
        return new li.cil.oc.core.impl.server.component.UpgradeDatabase(new DatabaseInventory() {
            private final ItemStack[] items = new ItemStack[OCSettings.get().databaseEntriesPerTier[tier()]];
            private final net.minecraft.core.HolderLookup.Provider lookupProvider = provider;

            {
                var c = container();
                if (c != null && !c.isEmpty()) {
                    reinitialize(provider);
                }
            }

            @Override
            public ItemStack container() {
                return stack;
            }

            @Override
            public ItemStack[] items() {
                return items;
            }

            @Override
            public void setChanged() {
                setChanged(lookupProvider);
            }

            @Override
            public void updateItems(int slot, ItemStack item) {
                if (slot >= 0 && slot < items.length) {
                    items[slot] = item;
                }
            }
        });
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.core.impl.common.item.UpgradeDatabase database) {
            return database.tier();
        }
        return Tier.One;
    }

    private static final DriverUpgradeDatabase INSTANCE = new DriverUpgradeDatabase();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.server.component.UpgradeDatabase.class;
            }
            return null;
        }
    }
}
