package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradeBattery extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.BatteryUpgradeTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.BatteryUpgradeTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.BatteryUpgradeTier3));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        return new li.cil.oc.core.impl.server.component.UpgradeBattery(tier(stack));
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.UpgradeBattery battery) {
            return battery.tier();
        }
        return Tier.One;
    }
}
