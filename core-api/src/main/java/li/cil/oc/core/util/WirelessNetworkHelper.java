package li.cil.oc.core.util;

import li.cil.oc.api.network.WirelessEndpoint;

public abstract class WirelessNetworkHelper {
    private static WirelessNetworkHelper instance;

    public static WirelessNetworkHelper get() {
        if (instance == null) {
            try {
                instance = (WirelessNetworkHelper) Class.forName("li.cil.oc.core.util.WirelessNetworkHelperImpl").getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
            }
        }
        return instance;
    }

    public abstract void add(WirelessEndpoint endpoint);

    public abstract void update(WirelessEndpoint endpoint);

    public abstract void remove(WirelessEndpoint endpoint);

    public abstract void remove(WirelessEndpoint endpoint, String dimension);

    public abstract Iterable<WirelessEndpoint> computeReachableFrom(WirelessEndpoint endpoint, double strength);
}
