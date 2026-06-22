package li.cil.oc.core.common.item.data;

import java.util.function.Supplier;

public final class NameProvider {
    private static Supplier<String> randomNameSupplier = () -> "";

    private NameProvider() {
    }

    public static void setRandomNameSupplier(Supplier<String> supplier) {
        randomNameSupplier = supplier;
    }

    public static String randomName() {
        return randomNameSupplier.get();
    }
}
