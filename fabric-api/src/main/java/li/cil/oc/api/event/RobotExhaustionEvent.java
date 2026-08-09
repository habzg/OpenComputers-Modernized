package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when a robot performed an action that would cause exhaustion for a
 * player. Used for the experience upgrade, for example.
 */
public class RobotExhaustionEvent extends RobotEvent {
    /**
     * The amount of exhaustion that was generated.
     */
    public final double exhaustion;

    public RobotExhaustionEvent(Agent agent, double exhaustion) {
        super(agent);
        this.exhaustion = exhaustion;
    }

    @FunctionalInterface
    public interface Listener {
        void onRobotExhaustion(RobotExhaustionEvent event);
    }

    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
        for (Listener listener : listeners) {
            listener.onRobotExhaustion(event);
        }
    });
}