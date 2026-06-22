package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverTpsCard extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.TpsCard));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        return new li.cil.oc.core.impl.server.component.TpsCard(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    private static final DriverTpsCard INSTANCE = new DriverTpsCard();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.server.component.TpsCard.class;
            }
            return null;
        }
    }
}
