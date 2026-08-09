package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public abstract class RobotBreakBlockEvent extends RobotEvent {
    protected RobotBreakBlockEvent(Agent agent) {
        super(agent);
    }

    /**
     * Fired when a robot is about to break a block.
     * <br>
     * Canceling the event will prevent the block from getting broken.
     */
    public static class Pre extends RobotBreakBlockEvent implements Cancelled {
        /**
         * The world in which the block will be broken.
         */
        public final Level world;

        /**
         * The coordinates at which the block will be broken.
         */
        public final BlockPos pos;

        /**
         * The time it takes to break the block.
         */
        private double breakTime;

        private boolean canceled;

        public Pre(Agent agent, Level world, BlockPos pos, double breakTime) {
            super(agent);
            this.world = world;
            this.pos = pos;
            this.breakTime = breakTime;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        /**
         * Sets the time it should take the robot to break the block.
         * <br>
         * Note that the robot will still break the block instantly, but the
         * robot's execution is paused for the specified amount of time.
         *
         * @param breakTime the time in seconds the break operation takes.
         */
        public void setBreakTime(double breakTime) {
            this.breakTime = Math.max(0.05, breakTime);
        }

        /**
         * Gets the time that it will take to break the block.
         *
         * @see #setBreakTime(double)
         */
        public double getBreakTime() {
            return breakTime;
        }

        @FunctionalInterface
        public interface Listener {
            void onRobotBreakBlock(Pre event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotBreakBlock(event);
                if (event.isCanceled()) break;
            }
        });
    }

    /**
     * Fired after a robot broke a block.
     */
    public static class Post extends RobotBreakBlockEvent {
        /**
         * The amount of experience the block that was broken generated (e.g. certain ores).
         */
        public final double experience;

        public Post(Agent agent, double experience) {
            super(agent);
            this.experience = experience;
        }

        @FunctionalInterface
        public interface Listener {
            void onRobotBreakBlock(Post event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotBreakBlock(event);
            }
        });
    }
}