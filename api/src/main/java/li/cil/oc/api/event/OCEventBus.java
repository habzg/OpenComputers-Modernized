package li.cil.oc.api.event;

/**
 * Loader-independent event bus for OpenComputers events.
 */
public final class OCEventBus {
    private OCEventBus() {
    }

    private static final java.util.Map<Class<? extends Event>, java.util.List<java.util.function.Consumer<? extends Event>>> listeners =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static Bridge bridge;

    /**
     * Bridge interface for forwarding OC events to a platform-specific
     * event bus (e.g. NeoForge's {@code EventBus}).
     */
    public interface Bridge {
        /**
         * Forward an event to the platform's event bus.
         *
         * @param event the event to forward
         * @param <T>   the event type
         * @return the event (possibly modified by platform listeners)
         */
        @SuppressWarnings("unused")
        <T extends Event> T forward(T event);
    }

    /**
     * Set the platform bridge. Called by loader-specific modules
     * during initialization.
     *
     * @param bridge the bridge, or {@code null} to remove
     */
    public static void setBridge(Bridge bridge) {
        OCEventBus.bridge = bridge;
    }

    /**
     * Register a listener for a specific event type.
     *
     * @param eventType the event class to listen for
     * @param listener  the listener to call when an event of the
     *                  specified type (or subtype) is posted
     * @param <T>       the event type
     */
    @SuppressWarnings("unused")
    public static <T extends Event> void addListener(Class<T> eventType, java.util.function.Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new java.util.ArrayList<>()).add(listener);
    }

    /**
     * Post an event to all registered listeners.
     * <p>
     * If a {@link Bridge} is set, the event is also forwarded to the
     * platform's event bus. The event is forwarded first, so platform
     * listeners can cancel it before OC-native listeners run.
     *
     * @param event the event to post
     * @param <T>   the event type
     * @return the event (for method chaining)
     */
    @SuppressWarnings("UnusedReturnValue")
    public static <T extends Event> T post(T event) {
        if (bridge != null) {
            event = bridge.forward(event);
        }

        dispatch(event, event.getClass());

        return event;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void dispatch(T event, Class<?> eventType) {
        if (eventType == null || !Event.class.isAssignableFrom(eventType)) {
            return;
        }

        java.util.List<java.util.function.Consumer<? extends Event>> listenersForType = listeners.get(eventType);
        if (listenersForType != null) {
            for (java.util.function.Consumer<? extends Event> listener : listenersForType) {
                ((java.util.function.Consumer<T>) listener).accept(event);
            }
        }

        dispatch(event, eventType.getSuperclass());
        for (Class<?> iface : eventType.getInterfaces()) {
            dispatch(event, iface);
        }
    }
}
