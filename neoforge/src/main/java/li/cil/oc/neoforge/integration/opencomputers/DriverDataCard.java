package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverDataCard extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.DataCardTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.DataCardTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.DataCardTier3));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        int t = tier(stack);
        if (t == Tier.Three) return new li.cil.oc.core.impl.server.component.DataCard.Tier3();
        if (t == Tier.Two) return new li.cil.oc.core.impl.server.component.DataCard.Tier2();
        return new li.cil.oc.core.impl.server.component.DataCard.Tier1();
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.DataCard data) {
            return data.tier();
        }
        return Tier.One;
    }

    private static final DriverDataCard INSTANCE = new DriverDataCard();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                int t = INSTANCE.tier(stack);
                if (t == Tier.Three) return li.cil.oc.core.impl.server.component.DataCard.Tier3.class;
                if (t == Tier.Two) return li.cil.oc.core.impl.server.component.DataCard.Tier2.class;
                return li.cil.oc.core.impl.server.component.DataCard.Tier1.class;
            }
            return null;
        }
    }
}
