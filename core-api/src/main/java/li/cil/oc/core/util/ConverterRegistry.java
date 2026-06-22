package li.cil.oc.core.util;

public abstract class ConverterRegistry {
    private static ConverterRegistry instance;

    public static void setInstance(ConverterRegistry inst) {
        instance = inst;
    }

    public static ConverterRegistry get() {
        return instance;
    }

    public abstract Object[] convert(Object[] values);
}
