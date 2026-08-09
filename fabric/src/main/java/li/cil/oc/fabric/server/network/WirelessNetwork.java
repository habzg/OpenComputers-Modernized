package li.cil.oc.fabric.server.network;

import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.impl.server.network.WirelessNetworkManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public final class WirelessNetwork {
    public static void init() {
        ServerWorldEvents.LOAD.register((server, world) -> WirelessNetworkManager.removeDimension(world.dimension()));

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!world.isClientSide()) {
                WirelessNetworkManager.removeDimension(world.dimension());
            }
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> chunk.getBlockEntities().values().stream()
                .filter(WirelessEndpoint.class::isInstance)
                .map(WirelessEndpoint.class::cast)
                .forEach(WirelessNetworkManager::remove));
    }
}
