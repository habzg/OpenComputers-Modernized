package li.cil.oc.neoforge.common;

import li.cil.oc.core.impl.server.component.UpgradeGenerator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class FuelProvider implements UpgradeGenerator.IFuelProvider {
    public static final FuelProvider INSTANCE = new FuelProvider();

    private FuelProvider() {
    }

    @Override
    public boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(null) > 0;
    }

    @Override
    public int getBurnTime(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING);
    }
}
