package li.cil.oc.neoforge.server.network;

import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.util.RTree;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class WirelessNetwork {
    private static final Map<ResourceKey<Level>, RTree<WirelessEndpoint>> dimensions = new HashMap<>();

    private WirelessNetwork() {
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldUnload(LevelEvent.Unload e) {
        Level level = (Level) e.getLevel();
        if (!level.isClientSide()) {
            dimensions.remove(level.dimension());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldLoad(LevelEvent.Load e) {
        Level level = (Level) e.getLevel();
        if (!level.isClientSide()) {
            dimensions.remove(level.dimension());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onChunkUnload(ChunkEvent.Unload e) {
        ((net.minecraft.world.level.chunk.LevelChunk) e.getChunk()).getBlockEntities().values().stream()
                .filter(WirelessEndpoint.class::isInstance)
                .map(WirelessEndpoint.class::cast)
                .forEach(WirelessNetwork::remove);
    }

    public static void add(WirelessEndpoint endpoint) {
        dimensions.computeIfAbsent(dimension(endpoint), k -> new RTree<>(Settings.get().rTreeMaxEntries, ep -> new double[]{ep.x() + 0.5, ep.y() + 0.5, ep.z() + 0.5}))
                .add(endpoint);
    }

    public static void update(WirelessEndpoint endpoint) {
        RTree<WirelessEndpoint> tree = dimensions.get(dimension(endpoint));
        if (tree != null) {
            double[] pos = tree.apply(endpoint);
            if (pos != null) {
                double dx = Math.abs(endpoint.x() + 0.5 - pos[0]);
                double dy = Math.abs(endpoint.y() + 0.5 - pos[1]);
                double dz = Math.abs(endpoint.z() + 0.5 - pos[2]);
                if (dx > 0.5 || dy > 0.5 || dz > 0.5) {
                    tree.remove(endpoint);
                    tree.add(endpoint);
                }
            }
        }
    }

    public static void remove(WirelessEndpoint endpoint, ResourceKey<Level> dimension) {
        RTree<WirelessEndpoint> set = dimensions.get(dimension);
        if (set != null) {
            set.remove(endpoint);
        }
    }

    public static RTree<WirelessEndpoint> getTree(ResourceKey<Level> dimension) {
        return dimensions.get(dimension);
    }

    public static void remove(WirelessEndpoint endpoint) {
        RTree<WirelessEndpoint> set = dimensions.get(dimension(endpoint));
        if (set != null) {
            set.remove(endpoint);
        }
    }

    public static Iterable<WirelessEndpoint> computeReachableFrom(WirelessEndpoint endpoint, double strength) {
        RTree<WirelessEndpoint> tree = dimensions.get(dimension(endpoint));
        if (tree != null && strength > 0) {
            double range = strength + 1;
            double[] min = offset(endpoint, -range);
            double[] max = offset(endpoint, range);
            List<WirelessEndpoint> candidates = new ArrayList<>();
            for (WirelessEndpoint ep : tree.query(min, max)) {
                if (ep != endpoint) candidates.add(ep);
            }
            List<Map.Entry<WirelessEndpoint, Double>> withDist = new ArrayList<>();
            for (WirelessEndpoint ep : candidates) {
                double distSq = squaredDistance(endpoint, ep);
                if (distSq <= range * range) {
                    withDist.add(new AbstractMap.SimpleEntry<>(ep, Math.sqrt(distSq)));
                }
            }
            List<WirelessEndpoint> result = new ArrayList<>();
            for (Map.Entry<WirelessEndpoint, Double> entry : withDist) {
                if (isUnobstructed(endpoint, strength, entry.getKey(), entry.getValue())) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static ResourceKey<Level> dimension(WirelessEndpoint endpoint) {
        return endpoint.level().dimension();
    }

    private static double[] offset(WirelessEndpoint endpoint, double value) {
        return new double[]{endpoint.x() + 0.5 + value, endpoint.y() + 0.5 + value, endpoint.z() + 0.5 + value};
    }

    private static double squaredDistance(WirelessEndpoint reference, WirelessEndpoint endpoint) {
        double dx = endpoint.x() - reference.x();
        double dy = endpoint.y() - reference.y();
        double dz = endpoint.z() - reference.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean isUnobstructed(WirelessEndpoint reference, double strength, WirelessEndpoint endpoint, double distance) {
        double gap = distance - 1;
        if (gap > 0) {
            Level world = reference.level();
            Vec3 origin = new Vec3(reference.x(), reference.y(), reference.z());
            Vec3 target = new Vec3(endpoint.x(), endpoint.y(), endpoint.z());
            Vec3 delta = target.subtract(origin);
            Vec3 v = delta.normalize();
            Vec3 up;
            if (v.x == 0 && v.z == 0) {
                up = new Vec3(1, 0, 0);
            } else {
                up = new Vec3(0, 1, 0);
            }
            Vec3 side = v.cross(up);
            Vec3 top = v.cross(side);
            double hardness = 0.0;
            int samples = Math.max(1, (int) Math.sqrt(gap));
            for (int i = 0; i < samples; i++) {
                double rGap = world.random.nextDouble() * gap;
                int rSide = world.random.nextInt(3) - 1;
                int rTop = world.random.nextInt(3) - 1;
                double x = origin.x + v.x * rGap + side.x * rSide + top.x * rTop;
                double y = origin.y + v.y * rGap + side.y * rSide + top.y * rTop;
                double z = origin.z + v.z * rGap + side.z * rSide + top.z * rTop;
                BlockPos pos = BlockPos.containing(x, y, z);
                if (world.isLoaded(pos)) {
                    hardness += world.getBlockState(pos).getDestroySpeed(world, pos);
                }
            }
            hardness *= gap / samples;
            return strength - gap > hardness;
        }
        return true;
    }
}
