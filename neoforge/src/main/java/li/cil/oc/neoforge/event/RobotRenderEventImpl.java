package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.internal.Agent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotRenderEventImpl extends Event implements RobotRenderEvent, ICancellableEvent {
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
    protected final MountPoint[] mountPoints;

    public RobotRenderEventImpl(Agent agent, MountPoint[] mountPoints) {
        this.agent = agent;
        this.mountPoints = mountPoints;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public MountPoint[] mountPoints() {
        return mountPoints;
    }
}
