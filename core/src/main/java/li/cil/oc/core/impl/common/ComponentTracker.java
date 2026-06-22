package li.cil.oc.core.impl.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public abstract class ComponentTracker {
    private static ComponentTracker serverTracker;

    public static void setServerTracker(ComponentTracker tracker) {
        serverTracker = tracker;
    }

    public static ComponentTracker getServerTracker() {
        return serverTracker;
    }

    private final Map<Integer, Cache<String, ManagedEnvironment>> worlds = new HashMap<>();

    private Cache<String, ManagedEnvironment> components(Level world) {
        return worlds.computeIfAbsent(world.dimension().location().hashCode(), k ->
                CacheBuilder.newBuilder().weakValues().build()
        );
    }

    public synchronized void add(Level world, String address, ManagedEnvironment component) {
        components(world).put(address, component);
    }

    public synchronized void remove(Level world, ManagedEnvironment component) {
        Cache<String, ManagedEnvironment> cache = components(world);
        cache.asMap().entrySet().removeIf(e -> e.getValue() == component);
        cache.cleanUp();
    }

    public synchronized ManagedEnvironment get(Level world, String address) {
        Cache<String, ManagedEnvironment> cache = components(world);
        cache.cleanUp();
        return cache.getIfPresent(address);
    }

    public synchronized void clear(Level world) {
        Cache<String, ManagedEnvironment> cache = components(world);
        cache.invalidateAll();
        cache.cleanUp();
    }
}
