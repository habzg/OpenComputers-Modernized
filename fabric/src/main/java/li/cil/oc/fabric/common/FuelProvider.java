package li.cil.oc.fabric.common;

import li.cil.oc.core.impl.server.component.UpgradeGenerator;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelProvider implements UpgradeGenerator.IFuelProvider {
    public static final FuelProvider INSTANCE = new FuelProvider();

    private FuelProvider() {
    }

    @Override
    public boolean isFuel(ItemStack stack) {
        Integer time = FuelRegistry.INSTANCE.get(stack.getItem());
        return time != null && time > 0;
    }

    @Override
    public int getBurnTime(ItemStack stack) {
        Integer time = FuelRegistry.INSTANCE.get(stack.getItem());
        return time != null ? time : 0;
    }
}
