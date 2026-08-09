package li.cil.oc.core.impl.util;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class EventHandlerDelegate {
    private static EventHandlerDelegate instance;

    public static void setInstance(EventHandlerDelegate inst) {
        instance = inst;
    }

    public static EventHandlerDelegate get() {
        return instance;
    }

    public abstract void scheduleServer(BlockEntity blockEntity);

    public abstract void scheduleServer(Runnable task);

    public abstract void onRobotStart(Object robot);

    public abstract void onRobotStopped(Object robot);

    public abstract void addKeyboard(li.cil.oc.core.impl.server.component.Keyboard keyboard);

    /**
     * Fired when a robot is about to move. The loader-specific implementation
     * creates the platform's move event and posts it to its event bus.
     *
     * @return whether the move is allowed to proceed (i.e. the event was not canceled).
     */
    public boolean postRobotMovePre(Agent robot, Direction direction) {
        return true;
    }

    /**
     * Fired after a robot moved.
     */
    public void postRobotMovePost(Agent robot, Direction direction) {
    }

    /**
     * Fired when a robot tries to place a block in thin air. The
     * loader-specific implementation creates the platform's place in air
     * event and posts it to its event bus.
     *
     * @return whether the placement is allowed.
     */
    public boolean postRobotPlaceInAir(Agent robot) {
        return false;
    }

    /**
     * Fired by the geolyzer for a long-distance scan. The loader-specific
     * implementation creates the platform's scan event and posts it to its
     * event bus, letting handlers fill the result data.
     *
     * @return the scan result data, or {@code null} if the event was canceled.
     */
    public float[] postGeolyzerScan(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new float[64];
    }

    /**
     * Fired by the geolyzer for a zero-range analysis. The loader-specific
     * implementation creates the platform's analyze event and posts it to its
     * event bus, letting handlers fill the result data.
     *
     * @return the analysis result data, or {@code null} if the event was canceled.
     */
    public Map<String, Object> postGeolyzerAnalyze(EnvironmentHost host, Map<?, ?> options, int x, int y, int z) {
        return new HashMap<>();
    }
}
