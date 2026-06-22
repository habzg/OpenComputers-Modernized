package li.cil.oc.neoforge.common.event;

import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.common.EventHandler;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

public final class BlockChangeHandler {
    private static final Map<ChangeListener, BlockPosition> changeListeners = new HashMap<>();

    public static void addListener(ChangeListener listener, BlockPosition coord) {
        EventHandler.scheduleServer(() -> changeListeners.put(listener, coord));
    }

    public static void removeListener(ChangeListener listener) {
        EventHandler.scheduleServer(() -> changeListeners.remove(listener));
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent e) {
        BlockPosition current = BlockPosition.apply(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), (Level) e.getLevel());
        for (Map.Entry<ChangeListener, BlockPosition> entry : changeListeners.entrySet()) {
            if (entry.getValue().equals(current)) {
                entry.getKey().onBlockChanged();
            }
        }
    }

    public interface ChangeListener {
        void onBlockChanged();
    }
}
