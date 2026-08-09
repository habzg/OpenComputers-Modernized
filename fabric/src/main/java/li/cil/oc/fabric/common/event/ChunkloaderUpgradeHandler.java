package li.cil.oc.fabric.common.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.UpgradeChunkloaderBase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ChunkloaderUpgradeHandler {
    private static final Map<Level, Map<String, Set<Long>>> worldChunks = new HashMap<>();

    public static void updateLoadedChunk(UpgradeChunkloaderBase loader) {
        var host = loader.host;
        var level = host.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        int centerChunkX = (int) Math.floor(host.xPosition()) >> 4;
        int centerChunkZ = (int) Math.floor(host.zPosition()) >> 4;
        String address = loader.node.address();

        var chunks = worldChunks.computeIfAbsent(level, k -> new HashMap<>());

        Set<Long> newChunks = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                newChunks.add(ChunkPos.asLong(centerChunkX + dx, centerChunkZ + dz));
            }
        }

        Set<Long> oldChunks = chunks.getOrDefault(address, Set.of());

        for (long chunk : oldChunks) {
            if (!newChunks.contains(chunk)) {
                serverLevel.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
            }
        }

        for (long chunk : newChunks) {
            if (!oldChunks.contains(chunk)) {
                serverLevel.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true);
            }
        }

        chunks.put(address, newChunks);
    }

    public static void releaseLoadedChunk(UpgradeChunkloaderBase loader) {
        var host = loader.host;
        var level = host.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        var worldMap = worldChunks.get(level);
        if (worldMap == null) return;

        var oldChunks = worldMap.remove(loader.node.address());
        if (oldChunks == null) return;

        for (long chunk : oldChunks) {
            serverLevel.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
        }

        if (worldMap.isEmpty()) {
            worldChunks.remove(level);
        }
    }

    public static void onMove(RobotMoveEvent.Post e) {
        Node machineNode = e.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof UpgradeChunkloaderBase) {
                updateLoadedChunk((UpgradeChunkloaderBase) node.host());
            }
        }
    }
}
