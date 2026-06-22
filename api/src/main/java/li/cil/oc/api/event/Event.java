package li.cil.oc.api.event;

/**
 * Base interface for all OpenComputers events.
 * <p>
 * This is a loader-independent event interface. Loader-specific modules
 * provide concrete implementations that extend the platform's native
 * event class (e.g. {@code net.neoforged.bus.api.Event} for NeoForge)
 * while implementing this interface.
 * <p>
 * Cancellable events should also implement {@link CancellableEvent}.
 */
public interface Event {
    /**
     * Returns whether this event has been cancelled.
     *
     * @return {@code true} if the event was cancelled by a listener.
     */
    boolean isCanceled();

    /**
     * Sets the cancelled state of this event.
     *
     * @param canceled {@code true} to cancel the event, {@code false} to un-cancel.
     */
    @SuppressWarnings("unused")
    void setCanceled(boolean canceled);
}
