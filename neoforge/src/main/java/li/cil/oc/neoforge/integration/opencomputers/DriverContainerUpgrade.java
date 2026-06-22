package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverContainerUpgrade extends Item implements Container {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.UpgradeContainerTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.UpgradeContainerTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.UpgradeContainerTier3));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Container;
    }

    @Override
    public String providedSlot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int providedTier(ItemStack stack) {
        return tier(stack);
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.UpgradeContainerUpgrade container) {
            return container.tier();
        }
        return Tier.One;
    }
}
