package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.Converter;
import net.minecraft.world.level.Level;

import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterWorld implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof Level world) {
            output.put("oc:flatten", world.dimension());
        }
    }
}
