package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverContainerCard extends Item implements Container {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.CardContainerTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.CardContainerTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.CardContainerTier3));
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
        return Slot.Card;
    }

    @Override
    public int providedTier(ItemStack stack) {
        return tier(stack);
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.UpgradeContainerCard container) {
            return container.tier();
        }
        return Tier.One;
    }
}
