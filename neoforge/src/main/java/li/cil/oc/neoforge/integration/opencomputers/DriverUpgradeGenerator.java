package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradeGenerator extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.GeneratorUpgrade));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        if (host instanceof li.cil.oc.api.internal.Agent) {
            return new li.cil.oc.core.impl.server.component.UpgradeGenerator((li.cil.oc.api.internal.Agent) host);
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        return Tier.Two;
    }

    private static final DriverUpgradeGenerator INSTANCE = new DriverUpgradeGenerator();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.server.component.UpgradeGenerator.class;
            }
            return null;
        }
    }
}
