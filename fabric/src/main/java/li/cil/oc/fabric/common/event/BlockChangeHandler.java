package li.cil.oc.fabric.common.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockChangeHandler {
    private static final Map<ChangeListener, TrackedPosition> changeListeners = new HashMap<>();

    public static void addListener(ChangeListener listener, BlockPosition coord) {
        synchronized (changeListeners) {
            changeListeners.put(listener, new TrackedPosition(coord));
        }
    }

    public static void removeListener(ChangeListener listener) {
        synchronized (changeListeners) {
            changeListeners.remove(listener);
        }
    }

    public static void tick() {
        synchronized (changeListeners) {
            Iterator<Map.Entry<ChangeListener, TrackedPosition>> it = changeListeners.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ChangeListener, TrackedPosition> entry = it.next();
                TrackedPosition tracked = entry.getValue();
                Level world = tracked.level();
                if (world == null) {
                    it.remove();
                    continue;
                }
                BlockPos pos = tracked.toBlockPos();
                BlockState currentState = world.getBlockState(pos);
                if (tracked.updateState(currentState)) {
                    entry.getKey().onBlockChanged();
                }
            }
        }
    }

    public interface ChangeListener {
        void onBlockChanged();
    }

    private static class TrackedPosition {
        final BlockPosition blockPosition;
        BlockState lastState;

        TrackedPosition(BlockPosition blockPosition) {
            this.blockPosition = blockPosition;
            this.lastState = null;
        }

        Level level() {
            return blockPosition.level();
        }

        BlockPos toBlockPos() {
            return new BlockPos(blockPosition.x(), blockPosition.y(), blockPosition.z());
        }

        boolean updateState(BlockState currentState) {
            if (lastState == null) {
                lastState = currentState;
                return false;
            }
            if (!lastState.equals(currentState)) {
                lastState = currentState;
                return true;
            }
            return false;
        }
    }
}
