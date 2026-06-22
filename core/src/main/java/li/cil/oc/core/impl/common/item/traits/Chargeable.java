package li.cil.oc.core.impl.common.item.traits;

import net.minecraft.world.item.ItemStack;

public interface Chargeable extends li.cil.oc.api.driver.item.Chargeable {
    double maxCharge(ItemStack ignoredStack);

    double getCharge(ItemStack stack);

    @SuppressWarnings("unused")
    void setCharge(ItemStack stack, double amount);

    default boolean canExtract(ItemStack ignoredStack) {
        return false;
    }
}
