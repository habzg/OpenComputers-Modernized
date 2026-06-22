package li.cil.oc.neoforge.integration.util;

import li.cil.oc.core.impl.Settings;

public final class Power {
    private Power() {
    }

    @SuppressWarnings("unused")
    public static double fromAE(double value) {
        return value * Settings.get().ratioAppliedEnergistics2();
    }

    @SuppressWarnings("unused")
    public static double toAE(double value) {
        return value / Settings.get().ratioAppliedEnergistics2();
    }

    public static double fromRF(double value) {
        return value * Settings.get().ratioRedstoneFlux();
    }

    public static int toRF(double value) {
        return (int) (value / Settings.get().ratioRedstoneFlux());
    }
}
