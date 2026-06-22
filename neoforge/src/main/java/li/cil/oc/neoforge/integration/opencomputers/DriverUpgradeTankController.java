package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.neoforge.common.tileentity.Robot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverUpgradeTankController extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.TankControllerUpgrade));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        return switch (host) {
            case Adapter adapter -> new li.cil.oc.neoforge.server.component.UpgradeTankController.Adapter(host);
            case Drone drone -> new li.cil.oc.neoforge.server.component.UpgradeTankController.Drone(drone);
            case Robot robot -> new li.cil.oc.neoforge.server.component.UpgradeTankController.Robot(robot);
            default -> null;
        };
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        return Tier.Two;
    }

    private static final DriverUpgradeTankController INSTANCE = new DriverUpgradeTankController();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.neoforge.server.component.UpgradeTankController.Robot.class;
            }
            return null;
        }
    }
}
