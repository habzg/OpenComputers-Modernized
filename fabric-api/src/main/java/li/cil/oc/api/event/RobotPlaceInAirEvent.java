package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * This event is fired when a robot tries to place a block and has no point of
 * reference, i.e. the place would have to be placed in "thin air". Per default
 * this fails (because players can't do this, either).
 * <br>
 * This is primarily intended for the 'Angel Upgrade', but it might be useful
 * for other upgrades, too.
 */
public class RobotPlaceInAirEvent extends RobotEvent implements Cancelled {
    private boolean isAllowed = false;
    private boolean canceled;

    public RobotPlaceInAirEvent(Agent agent) {
        super(agent);
    }

    /**
     * Whether the placement is allowed. Defaults to {@code false}.
     */
    public boolean isAllowed() {
        return isAllowed;
    }

    /**
     * Set whether the placement is allowed, can be used to allow robots to
     * place blocks in thin air.
     */
    public void setAllowed(boolean value) {
        this.isAllowed = value;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @FunctionalInterface
    public interface Listener {
        void onRobotPlaceInAir(RobotPlaceInAirEvent event);
    }

    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
        for (Listener listener : listeners) {
            listener.onRobotPlaceInAir(event);
            if (event.isCanceled()) break;
        }
    });
}