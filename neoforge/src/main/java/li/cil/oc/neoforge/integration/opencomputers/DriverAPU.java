package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverAPU extends DriverCPU implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.APUTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.APUTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.APUCreative));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        int gpu = gpuTier(stack);
        if (gpu == Tier.Three) return new li.cil.oc.core.impl.server.component.APU(Tier.Three);
        if (gpu == Tier.Two) return new li.cil.oc.core.impl.server.component.APU(Tier.Two);
        return new li.cil.oc.core.impl.server.component.APU(Tier.One);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.CPU;
    }

    public int supportedComponents(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.APU apu) {
            return Settings.get().cpuComponentSupport[apu.cpuTierForComponents()];
        }
        return Settings.get().cpuComponentSupport[1];
    }

    public int cpuTier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.APU apu) {
            return apu.cpuTier();
        }
        return Tier.One;
    }

    public int gpuTier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.APU apu) {
            return apu.gpuTier();
        }
        return Tier.One;
    }

    private static final DriverAPU INSTANCE = new DriverAPU();

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
