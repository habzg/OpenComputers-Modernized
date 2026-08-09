package li.cil.oc.core.impl.server;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.core.impl.common.blockentity.RobotProxy;
import net.minecraft.world.level.Level;

public final class ServerRobotRegistry {
    public static final ServerRobotRegistry INSTANCE = new ServerRobotRegistry();

    private final Map<Integer, Map<String, RobotProxy>> worlds = new HashMap<>();

    private Map<String, RobotProxy> proxies(Level level) {
        return worlds.computeIfAbsent(level.dimension().location().hashCode(), k -> new HashMap<>());
    }

    public synchronized void put(Level level, String address, RobotProxy proxy) {
        if (address == null || address.isEmpty()) return;
        proxies(level).put(address, proxy);
    }

    public synchronized RobotProxy get(Level level, String address) {
        if (address == null || address.isEmpty()) return null;
        return proxies(level).get(address);
    }

    public synchronized void remove(Level level, String address, RobotProxy proxy) {
        if (address == null || address.isEmpty()) return;
        var map = proxies(level);
        if (map.get(address) == proxy) map.remove(address);
    }

    public synchronized void clear(Level level) {
        worlds.remove(level.dimension().location().hashCode());
    }

    @SuppressWarnings("unused")
    public synchronized void clearAll() {
        worlds.clear();
    }
}
