package li.cil.oc.core.util;

import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.impl.server.network.WirelessNetworkManager;

@SuppressWarnings("unused")
public class WirelessNetworkHelperImpl extends WirelessNetworkHelper {
  @Override
  public void add(WirelessEndpoint endpoint) {
    WirelessNetworkManager.add(endpoint);
  }

  @Override
  public void update(WirelessEndpoint endpoint) {
    WirelessNetworkManager.update(endpoint);
  }

  @Override
  public void remove(WirelessEndpoint endpoint) {
    WirelessNetworkManager.remove(endpoint);
  }

  @Override
  public void remove(WirelessEndpoint endpoint, String dimension) {
    var key = net.minecraft.resources.ResourceKey.create(
      net.minecraft.core.registries.Registries.DIMENSION,
      net.minecraft.resources.ResourceLocation.parse(dimension));
    WirelessNetworkManager.remove(endpoint, key);
  }

  @Override
  public Iterable<WirelessEndpoint> computeReachableFrom(WirelessEndpoint endpoint, double strength) {
    return WirelessNetworkManager.computeReachableFrom(endpoint, strength);
  }
}
