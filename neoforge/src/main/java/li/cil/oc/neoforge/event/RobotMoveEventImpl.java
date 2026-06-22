package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotMoveEventImpl extends Event implements RobotMoveEvent, ICancellableEvent {
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
    protected final Direction direction;

    public RobotMoveEventImpl(Agent agent, Direction direction) {
        this.agent = agent;
        this.direction = direction;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public Direction direction() {
        return direction;
    }

    public static class Pre extends RobotMoveEventImpl implements RobotMoveEvent.Pre {
        public Pre(Agent agent, Direction direction) {
            super(agent, direction);
        }
    }

    public static class Post extends RobotMoveEventImpl implements RobotMoveEvent.Post {
        public Post(Agent agent, Direction direction) {
            super(agent, direction);
        }
    }
}
