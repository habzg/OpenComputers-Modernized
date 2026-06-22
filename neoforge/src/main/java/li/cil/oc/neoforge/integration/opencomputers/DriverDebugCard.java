package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverDebugCard extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.DebugCard));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        return new li.cil.oc.neoforge.server.component.DebugCard(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    private static final DriverDebugCard INSTANCE = new DriverDebugCard();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.neoforge.server.component.DebugCard.class;
            }
            return null;
        }
    }
}
