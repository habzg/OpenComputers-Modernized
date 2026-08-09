package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Direction;

public abstract class RobotMoveEvent extends RobotEvent {
    /**
     * The direction in which the robot will be moving.
     */
    public final Direction direction;

    protected RobotMoveEvent(Agent agent, Direction direction) {
        super(agent);
        this.direction = direction;
    }

    /**
     * Fired when a robot is about to move.
     * <br>
     * Canceling the event will prevent the robot from moving.
     */
    public static class Pre extends RobotMoveEvent implements Cancelled {
        private boolean canceled;

        public Pre(Agent agent, Direction direction) {
            super(agent, direction);
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        @FunctionalInterface
        public interface Listener {
            void onRobotMove(Pre event);
        }

        /**
         * Cancelable: canceling the event will prevent the robot from moving.
         */
        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotMove(event);
                if (event.isCanceled()) break;
            }
        });
    }

    /**
     * Fired after a robot moved.
     */
    public static class Post extends RobotMoveEvent {
        public Post(Agent agent, Direction direction) {
            super(agent, direction);
        }

        @FunctionalInterface
        public interface Listener {
            void onRobotMove(Post event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotMove(event);
            }
        });
    }
}