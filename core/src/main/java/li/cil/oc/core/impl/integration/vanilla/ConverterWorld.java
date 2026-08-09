package li.cil.oc.core.impl.integration.vanilla;

import java.util.Map;
import li.cil.oc.api.driver.Converter;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public final class ConverterWorld implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof Level world) {
            output.put("oc:flatten", world.dimension());
        }
    }
}
