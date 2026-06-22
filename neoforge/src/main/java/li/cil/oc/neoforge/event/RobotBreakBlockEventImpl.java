package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotBreakBlockEventImpl extends Event implements RobotBreakBlockEvent, ICancellableEvent {
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

    public RobotBreakBlockEventImpl(Agent agent) {
        this.agent = agent;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    public static class Pre extends RobotBreakBlockEventImpl implements RobotBreakBlockEvent.Pre {
        protected final Level level;
        protected final int x;
        protected final int y;
        protected final int z;
        private double breakTime;

        public Pre(Agent agent, Level world, int x, int y, int z, double breakTime) {
            super(agent);
            this.level = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.breakTime = breakTime;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
        }

        @Override
        public double getBreakTime() {
            return breakTime;
        }

        @Override
        public void setBreakTime(double breakTime) {
            this.breakTime = Math.max(0.05, breakTime);
        }
    }

    public static class Post extends RobotBreakBlockEventImpl implements RobotBreakBlockEvent.Post {
        protected final double experience;

        public Post(Agent agent, double experience) {
            super(agent);
            this.experience = experience;
        }

        @Override
        public double experience() {
            return experience;
        }
    }
}
