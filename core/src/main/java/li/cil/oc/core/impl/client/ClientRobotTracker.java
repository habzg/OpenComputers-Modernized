package li.cil.oc.core.impl.client;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.core.impl.common.blockentity.Robot;
import net.minecraft.world.level.Level;

public final class ClientRobotTracker {
    public static final ClientRobotTracker INSTANCE = new ClientRobotTracker();

    private final Map<Integer, Map<String, Robot>> worlds = new HashMap<>();

    private Map<String, Robot> robots(Level world) {
        return worlds.computeIfAbsent(world.dimension().location().hashCode(), k -> new HashMap<>());
    }

    public synchronized void add(Level world, String address, Robot robot) {
        robots(world).put(address, robot);
    }

    public synchronized void remove(Level world, String address) {
        robots(world).remove(address);
    }

    public synchronized void remove(Level world, Robot robot) {
        robots(world).entrySet().removeIf(e -> e.getValue() == robot);
    }

    public synchronized Robot get(Level world, String address) {
        return robots(world).get(address);
    }

    public synchronized void clear(Level world) {
        robots(world).clear();
    }

    public synchronized void clearAll() {
        worlds.clear();
    }
}
