package li.cil.oc.core.impl.integration.util;

import li.cil.oc.core.impl.server.component.RedstoneWireless;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WirelessRedstone {
    private static final Set<WirelessRedstoneSystem> systems = new LinkedHashSet<>();

    private WirelessRedstone() {
    }

    public static void register(WirelessRedstoneSystem system) {
        systems.add(system);
    }

    public static boolean isAvailable() {
        return !systems.isEmpty();
    }

    public static List<String> getProviderNames() {
        List<String> names = new ArrayList<>();
        for (WirelessRedstoneSystem system : systems) {
            names.add(system.name());
        }
        return names;
    }

    public static boolean hasProvider(String name) {
        for (WirelessRedstoneSystem system : systems) {
            if (system.name().equals(name)) return true;
        }
        return false;
    }

    public static boolean canHandleFrequency(int frequency) {
        for (WirelessRedstoneSystem system : systems) {
            if (system.canHandleFrequency(frequency)) return true;
        }
        return false;
    }

    public static boolean cannotHandleFrequency(int frequency, Set<String> enabledProviders) {
        if (enabledProviders == null) return !canHandleFrequency(frequency);
        for (WirelessRedstoneSystem system : systems) {
            if (enabledProviders.contains(system.name()) && system.canHandleFrequency(frequency)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canHandleFrequency(String providerName, int frequency) {
        for (WirelessRedstoneSystem system : systems) {
            if (system.name().equals(providerName)) {
                return system.canHandleFrequency(frequency);
            }
        }
        return false;
    }

    private static Set<WirelessRedstoneSystem> getEnabled(RedstoneWireless rs) {
        Set<String> enabled = rs.getEnabledProviders();
        if (enabled == null) return systems;
        Set<WirelessRedstoneSystem> result = new LinkedHashSet<>();
        for (WirelessRedstoneSystem system : systems) {
            if (enabled.contains(system.name())) {
                result.add(system);
            }
        }
        return result;
    }

    public static void addReceiver(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : getEnabled(rs)) {
            try {
                system.addReceiver(rs);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void removeReceiver(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : getEnabled(rs)) {
            try {
                system.removeReceiver(rs);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void updateOutput(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : getEnabled(rs)) {
            try {
                system.updateOutput(rs);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void removeTransmitter(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : getEnabled(rs)) {
            try {
                system.removeTransmitter(rs);
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean getInput(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : getEnabled(rs)) {
            if (system.getInput(rs)) return true;
        }
        return false;
    }

    public interface WirelessRedstoneSystem {
        String name();

        void addReceiver(RedstoneWireless rs);

        void removeReceiver(RedstoneWireless rs);

        void updateOutput(RedstoneWireless rs);

        void removeTransmitter(RedstoneWireless rs);

        boolean getInput(RedstoneWireless rs);

        @SuppressWarnings("unused")
        void resetRedstone(RedstoneWireless rs);

        boolean canHandleFrequency(int frequency);
    }
}
