package li.cil.oc.core.impl.integration.util;

import li.cil.oc.core.impl.OCSettings;

public final class Power {
    private Power() {
    }

    @SuppressWarnings("unused")
    public static double fromAE(double value) {
        return value * OCSettings.get().ratioAppliedEnergistics2();
    }

    @SuppressWarnings("unused")
    public static double toAE(double value) {
        return value / OCSettings.get().ratioAppliedEnergistics2();
    }

    public static double fromRF(double value) {
        return value * OCSettings.get().ratioRedstoneFlux();
    }

    public static int toRF(double value) {
        return (int) (value / OCSettings.get().ratioRedstoneFlux());
    }
}
