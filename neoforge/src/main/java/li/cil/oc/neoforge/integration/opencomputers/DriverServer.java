package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverServer extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.ServerTier1),
                Items.get(Constants.ItemName.ServerTier2),
                Items.get(Constants.ItemName.ServerTier3),
                Items.get(Constants.ItemName.ServerCreative));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host instanceof Rack rack) {
            int slot = indexOf(rack, stack);
            return new li.cil.oc.neoforge.server.component.Server(rack, slot);
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.RackMountable;
    }

    private static int indexOf(Rack rack, ItemStack stack) {
        for (int i = 0; i < rack.getContainerSize(); i++) {
            if (rack.getItem(i) == stack) return i;
        }
        return -1;
    }

    private static final DriverServer INSTANCE = new DriverServer();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.neoforge.server.component.Server.class;
            }
            return null;
        }
    }
}
