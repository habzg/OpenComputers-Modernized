package li.cil.oc.api.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface RobotPlaceBlockEvent extends RobotEvent {
    /**
     * The item that is used to place the block.
     */
    @SuppressWarnings("unused")
    ItemStack stack();

    /**
     * The Level in which the block will be placed.
     */
    @SuppressWarnings("unused")
    Level level();

    /**
     * The x coordinate at which the block will be placed.
     */
    @SuppressWarnings("unused")
    int x();

    /**
     * The y coordinate at which the block will be placed.
     */
    @SuppressWarnings("unused")
    int y();

    /**
     * The z coordinate at which the block will be placed.
     */
    @SuppressWarnings("unused")
    int z();

    /**
     * Fired when a robot is about to place a block.
     * <br>
     * Canceling this event will prevent the block from being placed.
     */
    interface Pre extends RobotPlaceBlockEvent, CancellableEvent {
    }

    /**
     * Fired after a robot placed a block.
     */
    interface Post extends RobotPlaceBlockEvent {
    }
}
