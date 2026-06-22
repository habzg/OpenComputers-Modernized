package li.cil.oc.neoforge.integration.opencomputers;

import com.google.common.base.Strings;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.Constants;
import li.cil.oc.neoforge.common.item.data.NanomachineData;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterNanomachines implements Converter {
    private final li.cil.oc.api.detail.ItemInfo nanomachines = li.cil.oc.api.Items.get(Constants.ItemName.Nanomachines);

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack && li.cil.oc.api.Items.get(stack) == nanomachines) {
            NanomachineData data = new NanomachineData(stack);
            if (!Strings.isNullOrEmpty(data.uuid)) {
                output.put("nanomachines", data.uuid);
            }
        }
    }
}
