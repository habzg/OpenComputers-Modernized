package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

public class RobotAttackEntityEvent extends RobotEvent {
    /**
     * The entity that the robot will attack.
     */
    public final Entity target;

    protected RobotAttackEntityEvent(Agent agent, Entity target) {
        super(agent);
        this.target = target;
    }

    /**
     * Fired when a robot is about to attack an entity.
     * <br>
     * Canceling the event will prevent the attack.
     */
    public static class Pre extends RobotAttackEntityEvent implements Cancelled {
        private boolean canceled;

        public Pre(Agent agent, Entity target) {
            super(agent, target);
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
            void onRobotAttackEntity(Pre event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotAttackEntity(event);
                if (event.isCanceled()) break;
            }
        });
    }

    /**
     * Fired after a robot has attacked an entity.
     */
    public static class Post extends RobotAttackEntityEvent {
        public Post(Agent agent, Entity target) {
            super(agent, target);
        }

        @FunctionalInterface
        public interface Listener {
            void onRobotAttackEntity(Post event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotAttackEntity(event);
            }
        });
    }
}