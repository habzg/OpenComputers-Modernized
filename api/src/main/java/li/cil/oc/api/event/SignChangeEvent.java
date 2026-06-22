package li.cil.oc.api.event;

import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * A bit more specific sign change event that holds information about new text of the sign. Used in the sign upgrade.
 */
public interface SignChangeEvent extends Event, CancellableEvent {
    /**
     * The sign block entity being modified.
     */
    @SuppressWarnings("unused")
    SignBlockEntity sign();

    /**
     * The new lines of text for the sign.
     */
    @SuppressWarnings("unused")
    String[] lines();

    /**
     * Fired before the sign text is changed.
     */
    interface Pre extends SignChangeEvent {
    }

    /**
     * Fired after the sign text is changed.
     */
    interface Post extends SignChangeEvent {
    }
}
