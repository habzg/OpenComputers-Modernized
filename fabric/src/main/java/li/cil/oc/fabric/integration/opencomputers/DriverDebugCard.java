package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.fabric.server.component.DebugCard;
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
        return new DebugCard(host);
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
                return li.cil.oc.fabric.server.component.DebugCard.class;
            }
            return null;
        }
    }
}
