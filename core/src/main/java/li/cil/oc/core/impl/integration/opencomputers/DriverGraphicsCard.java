package li.cil.oc.core.impl.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverGraphicsCard extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.GraphicsCardTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.GraphicsCardTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.GraphicsCardTier3));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        int t = INSTANCE.tier(stack);
        if (t == Tier.Three) return new li.cil.oc.core.impl.server.component.GraphicsCard(Tier.Three);
        if (t == Tier.Two) return new li.cil.oc.core.impl.server.component.GraphicsCard(Tier.Two);
        return new li.cil.oc.core.impl.server.component.GraphicsCard(Tier.One);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.core.impl.common.item.GraphicsCard gpu) {
            return gpu.tier();
        }
        return Tier.One;
    }

    private static final DriverGraphicsCard INSTANCE = new DriverGraphicsCard();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.server.component.GraphicsCard.class;
            }
            return null;
        }
    }
}
