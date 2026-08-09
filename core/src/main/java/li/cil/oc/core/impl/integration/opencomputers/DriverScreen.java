package li.cil.oc.core.impl.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.common.blockentity.Screen;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverScreen extends Item implements HostAware {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.BlockName.ScreenTier1));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host instanceof Screen && ((Screen) host).tier > 0) {
            return new li.cil.oc.core.impl.common.component.Screen((Screen) host);
        }
        return new li.cil.oc.core.impl.common.component.TextBuffer(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    private static final DriverScreen INSTANCE = new DriverScreen();

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (INSTANCE.worksWith(stack)) {
                return li.cil.oc.core.impl.common.component.Screen.class;
            }
            return null;
        }
    }
}
