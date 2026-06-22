package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotEvent;
import li.cil.oc.api.internal.Agent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@SuppressWarnings("unused")
public class RobotEventImpl extends Event implements RobotEvent, ICancellableEvent {
    protected final Agent agent;

    @SuppressWarnings("unused")
    public RobotEventImpl(Agent agent) {
        this.agent = agent;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean canceled) {
        ICancellableEvent.super.setCanceled(canceled);
    }
}
