package li.cil.oc.neoforge.server.network;

import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.server.network.WaypointManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class Waypoints {
    private Waypoints() {
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldUnload(LevelEvent.Unload e) {
        Level level = (Level) e.getLevel();
        if (!level.isClientSide()) {
            WaypointManager.removeDimension(level.dimension());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldLoad(LevelEvent.Load e) {
        Level level = (Level) e.getLevel();
        if (!level.isClientSide()) {
            WaypointManager.removeDimension(level.dimension());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onChunkUnload(ChunkEvent.Unload e) {
        ((net.minecraft.world.level.chunk.LevelChunk) e.getChunk()).getBlockEntities().values().stream()
                .filter(Waypoint.class::isInstance)
                .map(Waypoint.class::cast)
                .forEach(WaypointManager::remove);
    }
}
