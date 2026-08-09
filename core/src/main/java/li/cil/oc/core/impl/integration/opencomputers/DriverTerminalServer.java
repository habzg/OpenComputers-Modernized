package li.cil.oc.core.impl.integration.opencomputers;

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
public final class DriverTerminalServer extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, Items.get(Constants.ItemName.TerminalServer));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host instanceof Rack rack) {
            int slot = indexOf(rack, stack);
            return new li.cil.oc.core.impl.common.component.TerminalServer(rack, slot);
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

    private static final DriverTerminalServer INSTANCE = new DriverTerminalServer();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.common.component.TerminalServer.class;
            }
            return null;
        }
    }
}
