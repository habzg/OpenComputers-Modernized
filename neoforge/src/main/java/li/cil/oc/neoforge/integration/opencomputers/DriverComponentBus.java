package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverComponentBus extends Item implements Processor {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.ComponentBusTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.ComponentBusTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.ComponentBusTier3),
                li.cil.oc.api.Items.get(Constants.ItemName.ComponentBusCreative));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.ComponentBus;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.ComponentBus bus) {
            return Math.min(bus.tier(), Tier.Three);
        }
        return Tier.One;
    }

    @Override
    public int supportedComponents(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.ComponentBus bus) {
            return Settings.get().cpuComponentSupport[bus.tier()];
        }
        return Settings.get().cpuComponentSupport[Tier.One];
    }

    @Override
    public Class<? extends li.cil.oc.api.machine.Architecture> architecture(ItemStack stack) {
        return null;
    }
}
