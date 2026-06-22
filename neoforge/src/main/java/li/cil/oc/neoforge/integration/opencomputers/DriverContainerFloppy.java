package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverContainerFloppy extends Item implements Container {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.BlockName.DiskDrive));
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
        return Slot.Floppy;
    }

    @Override
    public int providedTier(ItemStack stack) {
        return Tier.Any;
    }
}
