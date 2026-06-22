package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverWirelessNetworkCard extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.WirelessNetworkCardTier2));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        int t = INSTANCE.tier(stack);
        if (t == Tier.Two) {
            return new li.cil.oc.core.impl.server.component.WirelessNetworkCard.Tier2(host);
        }
        return new li.cil.oc.core.impl.server.component.WirelessNetworkCard.Tier1(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.WirelessNetworkCard card) {
            return card.tier();
        }
        return Tier.One;
    }

    private static final DriverWirelessNetworkCard INSTANCE = new DriverWirelessNetworkCard();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                int t = INSTANCE.tier(stack);
                if (t == Tier.Two) {
                    return li.cil.oc.core.impl.server.component.WirelessNetworkCard.Tier2.class;
                }
                return li.cil.oc.core.impl.server.component.WirelessNetworkCard.Tier1.class;
            }
            return null;
        }
    }
}
