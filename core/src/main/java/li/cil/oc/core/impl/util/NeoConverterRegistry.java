package li.cil.oc.core.impl.util;

import li.cil.oc.core.util.ConverterRegistry;

public class NeoConverterRegistry extends ConverterRegistry {
    @Override
    public Object[] convert(Object[] values) {
        return li.cil.oc.core.impl.server.driver.Registry.INSTANCE.convert(values);
    }
}
