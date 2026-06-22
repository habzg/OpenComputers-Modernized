package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.internal.Agent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotPlaceInAirEventImpl extends Event implements RobotPlaceInAirEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final Agent agent;
    private boolean isAllowed = false;

    public RobotPlaceInAirEventImpl(Agent agent) {
        this.agent = agent;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public boolean isAllowed() {
        return isAllowed;
    }

    @Override
    public void setAllowed(boolean value) {
        this.isAllowed = value;
    }
}
