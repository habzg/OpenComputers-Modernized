package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverKeyboard extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.BlockName.Keyboard));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new li.cil.oc.core.impl.server.component.Keyboard(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }
}
