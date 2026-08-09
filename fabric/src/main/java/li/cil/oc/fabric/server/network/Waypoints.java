package li.cil.oc.fabric.server.network;

import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.server.network.WaypointManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public final class Waypoints {
    public static void init() {
        ServerWorldEvents.LOAD.register((server, world) -> WaypointManager.removeDimension(world.dimension()));

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!world.isClientSide()) {
                WaypointManager.removeDimension(world.dimension());
            }
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> chunk.getBlockEntities().values().stream()
                .filter(Waypoint.class::isInstance)
                .map(Waypoint.class::cast)
                .forEach(WaypointManager::remove));
    }
}
