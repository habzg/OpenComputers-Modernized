package li.cil.oc.api.event;

/**
 * Fired when a robot performed an action that would cause exhaustion for a
 * player. Used for the experience upgrade, for example.
 */
public interface RobotExhaustionEvent extends RobotEvent {
    /**
     * The amount of exhaustion that was generated.
     */
    @SuppressWarnings("unused")
    double exhaustion();
}
