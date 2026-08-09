package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.blockentity.traits.BundledRedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.integration.util.WirelessRedstone;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverRedstoneCard extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.RedstoneCardTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.RedstoneCardTier2));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        boolean isAdvanced = INSTANCE.tier(stack) == Tier.Two;
        boolean hasBundled = BundledRedstone.isAvailable() && isAdvanced;
        boolean hasWireless = WirelessRedstone.isAvailable() && isAdvanced;
        if (host instanceof BundledRedstoneAware && hasBundled) {
            if (hasWireless)
                return new li.cil.oc.core.impl.server.component.Redstone.BundledWireless(host);
            else return new li.cil.oc.core.impl.server.component.Redstone.Bundled(host);
        } else if (host instanceof RedstoneAware) {
            if (hasWireless) return new li.cil.oc.core.impl.server.component.Redstone.VanillaWireless(host);
            else return new li.cil.oc.core.impl.server.component.Redstone.Vanilla(host);
        } else {
            if (hasWireless) return new li.cil.oc.core.impl.server.component.Redstone.Wireless(host);
            else return null;
        }
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.core.impl.common.item.RedstoneCard card) {
            return card.tier;
        }
        return Tier.One;
    }

    private static final DriverRedstoneCard INSTANCE = new DriverRedstoneCard();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                boolean isAdvanced = INSTANCE.tier(stack) == Tier.Two;
                boolean hasBundled = BundledRedstone.isAvailable() && isAdvanced;
                boolean hasWireless = WirelessRedstone.isAvailable() && isAdvanced;
                if (hasBundled) {
                    if (hasWireless) return li.cil.oc.core.impl.server.component.Redstone.BundledWireless.class;
                    else return li.cil.oc.core.impl.server.component.Redstone.Bundled.class;
                } else {
                    return li.cil.oc.core.impl.server.component.Redstone.Vanilla.class;
                }
            }
            return null;
        }
    }
}
