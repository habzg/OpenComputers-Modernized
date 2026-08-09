package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.common.item.TabletWrapper;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.core.impl.server.component.UpgradeTractorBeam;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradeTractorBeam extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.TractorBeamUpgrade));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        return switch (host) {
            case Drone drone -> new UpgradeTractorBeam.Drone(drone);
            case Robot robot -> new UpgradeTractorBeam.Player(host, robot::player);
            case TabletWrapper tabletWrapper ->
                    new UpgradeTractorBeam.Player(host, () -> ((TabletWrapper) host).player());
            default -> null;
        };
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        return Tier.Three;
    }

    private static final DriverUpgradeTractorBeam INSTANCE = new DriverUpgradeTractorBeam();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.server.component.UpgradeTractorBeam.Common.class;
            }
            return null;
        }
    }
}
