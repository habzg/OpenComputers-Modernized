package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.util.DriverScreenHelper;
import li.cil.oc.neoforge.integration.opencomputers.DriverScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class NeoDriverScreenHelper extends DriverScreenHelper {
    @Override
    public boolean isDriverScreen(Object driver) {
        return driver instanceof DriverScreen;
    }

    @Override
    public void clearDataTag(Object driver, ItemStack stack) {
        if (driver instanceof DriverScreen ds) {
            var nbt = ds.dataTag(stack);
            for (var tagName : List.copyOf(nbt.getAllKeys())) {
                nbt.remove(tagName);
            }
        }
    }
}
