package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverMemory extends Item implements li.cil.oc.api.driver.item.Memory, li.cil.oc.api.driver.item.CallBudget {
    @Override
    public double amount(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.Memory memory) {
            return Settings.get().ramSizes[Math.clamp(memory.tier(), 0, Settings.get().ramSizes.length - 1)];
        }
        return 0.0;
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.RAMTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.RAMTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.RAMTier3),
                li.cil.oc.api.Items.get(Constants.ItemName.RAMTier4),
                li.cil.oc.api.Items.get(Constants.ItemName.RAMTier5),
                li.cil.oc.api.Items.get(Constants.ItemName.RAMTier6));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) {
        return new li.cil.oc.core.impl.server.component.Memory(tier(stack));
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Memory;
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.neoforge.common.item.Memory memory) {
            return memory.tier() / 2;
        }
        return Tier.One;
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        return Settings.get().callBudgets[Math.clamp(tier(stack), Tier.One, Tier.Three)];
    }
}
