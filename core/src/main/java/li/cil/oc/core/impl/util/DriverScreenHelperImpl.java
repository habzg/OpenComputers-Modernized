package li.cil.oc.core.impl.util;

import java.util.List;
import li.cil.oc.core.impl.integration.opencomputers.DriverScreen;
import net.minecraft.world.item.ItemStack;

public class DriverScreenHelperImpl extends DriverScreenHelper {
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
