package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverDiskDriveMountable extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, Items.get(Constants.ItemName.DiskDriveMountable));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host instanceof li.cil.oc.api.internal.Rack rack) {
            int slot = indexOf(rack, stack);
            return new li.cil.oc.fabric.server.component.DiskDriveMountable(rack, slot);
        }
        return null;
    }

    private static int indexOf(li.cil.oc.api.internal.Rack rack, ItemStack stack) {
        for (int i = 0; i < rack.getContainerSize(); i++) {
            if (rack.getItem(i) == stack) return i;
        }
        return -1;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.RackMountable;
    }

    private static final DriverDiskDriveMountable INSTANCE = new DriverDiskDriveMountable();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.fabric.server.component.DiskDriveMountable.class;
            }
            return null;
        }
    }
}
