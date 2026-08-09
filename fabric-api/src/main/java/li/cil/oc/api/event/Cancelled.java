package li.cil.oc.api.event;

/**
 * Marker interface for events that can be canceled.
 */
interface Cancelled {
    boolean isCanceled();

    void setCanceled(boolean canceled);
}