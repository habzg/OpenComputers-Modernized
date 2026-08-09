package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class RobotPlaceBlockEvent extends RobotEvent {
    /**
     * The item that is used to place the block.
     */
    public final ItemStack stack;

    /**
     * The world in which the block will be placed.
     */
    public final Level world;

    /**
     * The coordinates at which the block will be placed.
     */
    public final BlockPos pos;

    protected RobotPlaceBlockEvent(Agent agent, ItemStack stack, Level world, BlockPos pos) {
        super(agent);
        this.stack = stack;
        this.world = world;
        this.pos = pos;
    }

    /**
     * Fired when a robot is about to place a block.
     * <br>
     * Canceling the event will prevent the block from being placed.
     */
    public static class Pre extends RobotPlaceBlockEvent implements Cancelled {
        private boolean canceled;

        public Pre(Agent agent, ItemStack stack, Level world, BlockPos pos) {
            super(agent, stack, world, pos);
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
            void onRobotPlaceBlock(Pre event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotPlaceBlock(event);
                if (event.isCanceled()) break;
            }
        });
    }

    /**
     * Fired after a robot placed a block.
     */
    public static class Post extends RobotPlaceBlockEvent {
        public Post(Agent agent, ItemStack stack, Level world, BlockPos pos) {
            super(agent, stack, world, pos);
        }

        @FunctionalInterface
        public interface Listener {
            void onRobotPlaceBlock(Post event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRobotPlaceBlock(event);
            }
        });
    }
}