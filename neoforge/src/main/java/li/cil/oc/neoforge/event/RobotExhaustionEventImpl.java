package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.internal.Agent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotExhaustionEventImpl extends Event implements RobotExhaustionEvent, ICancellableEvent {
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
    protected final double exhaustion;

    public RobotExhaustionEventImpl(Agent agent, double exhaustion) {
        this.agent = agent;
        this.exhaustion = exhaustion;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public double exhaustion() {
        return exhaustion;
    }
}
