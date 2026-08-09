package li.cil.oc.core.impl.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradePiston extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.PistonUpgrade))
                || isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.StickyPistonUpgrade));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        boolean sticky = isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.StickyPistonUpgrade));
        return switch (host) {
            case li.cil.oc.api.internal.Drone drone ->
                    sticky ? new li.cil.oc.core.impl.server.component.UpgradeStickyPiston.Drone(drone)
                            : new li.cil.oc.core.impl.server.component.UpgradePiston.Drone(drone);
            case li.cil.oc.api.internal.Tablet tablet ->
                    sticky ? new li.cil.oc.core.impl.server.component.UpgradeStickyPiston.Tablet(tablet)
                            : new li.cil.oc.core.impl.server.component.UpgradePiston.Tablet(tablet);
            case li.cil.oc.api.internal.Rotatable rotatable ->
                    sticky ? new li.cil.oc.core.impl.server.component.UpgradeStickyPiston.Rotatable(rotatable)
                            : new li.cil.oc.core.impl.server.component.UpgradePiston.Rotatable(rotatable);
            default -> null;
        };
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    private static final DriverUpgradePiston INSTANCE = new DriverUpgradePiston();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                boolean sticky = INSTANCE.isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.StickyPistonUpgrade));
                return sticky ? li.cil.oc.core.impl.server.component.UpgradeStickyPiston.class
                        : li.cil.oc.core.impl.server.component.UpgradePiston.class;
            }
            return null;
        }
    }
}
