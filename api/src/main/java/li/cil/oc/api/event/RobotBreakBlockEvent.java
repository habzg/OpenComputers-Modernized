package li.cil.oc.api.event;

import net.minecraft.world.level.Level;

public interface RobotBreakBlockEvent extends RobotEvent {
    /**
     * Fired when a robot is about to break a block.
     * <br>
     * Canceling this event will prevent the block from getting broken.
     */
    interface Pre extends RobotBreakBlockEvent, CancellableEvent {
        /**
         * The Level in which the block will be broken.
         */
        @SuppressWarnings("unused")
        Level level();

        /**
         * The x coordinate at which the block will be broken.
         */
        @SuppressWarnings("unused")
        int x();

        /**
         * The y coordinate at which the block will be broken.
         */
        @SuppressWarnings("unused")
        int y();

        /**
         * The z coordinate at which the block will be broken.
         */
        @SuppressWarnings("unused")
        int z();

        /**
         * Gets the time that it will take to break the block.
         *
         * @see #setBreakTime(double)
         */
        @SuppressWarnings("unused")
        double getBreakTime();

        /**
         * Sets the time it should take the robot to break the block.
         * <br>
         * Note that the robot will still break the block instantly, but the
         * robot's execution is paused for the specified amount of time.
         *
         * @param breakTime the time in seconds the break operation takes.
         */
        @SuppressWarnings("unused")
        void setBreakTime(double breakTime);
    }

    /**
     * Fired after a robot broke a block.
     */
    interface Post extends RobotBreakBlockEvent {
        /**
         * The amount of experience the block that was broken generated (e.g. certain ores).
         */
        @SuppressWarnings("unused")
        double experience();
    }
}
