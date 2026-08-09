package li.cil.oc.neoforge.server.network;

import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.impl.server.network.WirelessNetworkManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class WirelessNetwork {
    private WirelessNetwork() {
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldUnload(LevelEvent.Unload e) {
        Level level = (Level) e.getLevel();
        if (!level.isClientSide()) {
            WirelessNetworkManager.removeDimension(level.dimension());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldLoad(LevelEvent.Load e) {
        Level level = (Level) e.getLevel();
        if (!level.isClientSide()) {
            WirelessNetworkManager.removeDimension(level.dimension());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onChunkUnload(ChunkEvent.Unload e) {
        ((net.minecraft.world.level.chunk.LevelChunk) e.getChunk()).getBlockEntities().values().stream()
                .filter(WirelessEndpoint.class::isInstance)
                .map(WirelessEndpoint.class::cast)
                .forEach(WirelessNetworkManager::remove);
    }
}
