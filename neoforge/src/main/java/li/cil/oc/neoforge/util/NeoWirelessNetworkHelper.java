package li.cil.oc.neoforge.util;

import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.util.WirelessNetworkHelper;
import li.cil.oc.neoforge.server.network.WirelessNetwork;

public class NeoWirelessNetworkHelper extends WirelessNetworkHelper {
    public NeoWirelessNetworkHelper() {
        WirelessNetworkHelper.setInstance(this);
    }

    @Override
    public void add(WirelessEndpoint endpoint) {
        WirelessNetwork.add(endpoint);
    }

    @Override
    public void update(WirelessEndpoint endpoint) {
        WirelessNetwork.update(endpoint);
    }

    @Override
    public void remove(WirelessEndpoint endpoint) {
        WirelessNetwork.remove(endpoint);
    }

    @Override
    public void remove(WirelessEndpoint endpoint, String dimension) {
        var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(dimension));
        WirelessNetwork.remove(endpoint, key);
    }

    @Override
    public Iterable<WirelessEndpoint> computeReachableFrom(WirelessEndpoint endpoint, double strength) {
        return WirelessNetwork.computeReachableFrom(endpoint, strength);
    }
}
