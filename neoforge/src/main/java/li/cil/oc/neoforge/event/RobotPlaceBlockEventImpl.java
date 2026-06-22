package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotPlaceBlockEventImpl extends Event implements RobotPlaceBlockEvent, ICancellableEvent {
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
    protected final ItemStack stack;
    protected final Level level;
    protected final int x;
    protected final int y;
    protected final int z;

    public RobotPlaceBlockEventImpl(Agent agent, ItemStack stack, Level world, int x, int y, int z) {
        this.agent = agent;
        this.stack = stack;
        this.level = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public ItemStack stack() {
        return stack;
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

    public static class Pre extends RobotPlaceBlockEventImpl implements RobotPlaceBlockEvent.Pre {
        public Pre(Agent agent, ItemStack stack, Level world, int x, int y, int z) {
            super(agent, stack, world, x, y, z);
        }
    }

    public static class Post extends RobotPlaceBlockEventImpl implements RobotPlaceBlockEvent.Post {
        public Post(Agent agent, ItemStack stack, Level world, int x, int y, int z) {
            super(agent, stack, world, x, y, z);
        }
    }
}
