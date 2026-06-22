package li.cil.oc.api.event;

import net.minecraft.core.Direction;

public interface RobotMoveEvent extends RobotEvent {
    /**
     * The direction in which the robot will be moving.
     */
    @SuppressWarnings("unused")
    Direction direction();

    /**
     * Fired when a robot is about to move.
     * <br>
     * Canceling the event will prevent the robot from moving.
     */
    interface Pre extends RobotMoveEvent, CancellableEvent {
    }

    /**
     * Fired after a robot moved.
     */
    interface Post extends RobotMoveEvent {
    }
}
