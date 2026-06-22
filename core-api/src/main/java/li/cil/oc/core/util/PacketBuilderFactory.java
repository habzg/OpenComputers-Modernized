package li.cil.oc.core.util;

import li.cil.oc.core.common.PacketType;

public abstract class PacketBuilderFactory {
    private static PacketBuilderFactory instance;

    public static void setInstance(PacketBuilderFactory inst) {
        instance = inst;
    }

    public static PacketBuilderFactory get() {
        return instance;
    }

    public abstract Object createCompressed(PacketType type);
}
