package li.cil.oc.api.event;

import net.minecraft.world.entity.Entity;

public interface RobotAttackEntityEvent extends RobotEvent {
    /**
     * The entity that the robot will attack.
     */
    @SuppressWarnings("unused")
    Entity target();

    /**
     * Fired when a robot is about to attack an entity.
     * <br>
     * Canceling this event will prevent the attack.
     */
    interface Pre extends RobotAttackEntityEvent, CancellableEvent {
    }

    /**
     * Fired after a robot has attacked an entity.
     */
    interface Post extends RobotAttackEntityEvent {
    }
}
