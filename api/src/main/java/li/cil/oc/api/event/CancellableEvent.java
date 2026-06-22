package li.cil.oc.api.event;

/**
 * Marker interface for cancellable events.
 * <p>
 * Events that implement this interface can be cancelled by listeners
 * via {@link Event#setCanceled(boolean)}, preventing the default
 * behavior from executing. The event bus checks
 * {@link Event#isCanceled()} after dispatch and skips the default
 * behavior if the event was cancelled.
 * <p>
 * This is loader-independent. Loader-specific modules bridge this to
 * the platform's native cancellation mechanism.
 */
public interface CancellableEvent {
}
