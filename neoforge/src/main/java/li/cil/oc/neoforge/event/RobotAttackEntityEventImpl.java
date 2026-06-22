package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotAttackEntityEventImpl extends Event implements RobotAttackEntityEvent, ICancellableEvent {
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
    protected final Entity target;

    public RobotAttackEntityEventImpl(Agent agent, Entity target) {
        this.agent = agent;
        this.target = target;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public Entity target() {
        return target;
    }

    public static class Pre extends RobotAttackEntityEventImpl implements RobotAttackEntityEvent.Pre {
        public Pre(Agent agent, Entity target) {
            super(agent, target);
        }
    }

    public static class Post extends RobotAttackEntityEventImpl implements RobotAttackEntityEvent.Post {
        public Post(Agent agent, Entity target) {
            super(agent, target);
        }
    }
}
