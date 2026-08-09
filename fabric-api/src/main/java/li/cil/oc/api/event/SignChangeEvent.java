package li.cil.oc.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * A bit more specific sign change event that holds information about new text of the sign. Used in the sign upgrade.
 */
public abstract class SignChangeEvent implements Cancelled {
    private boolean canceled;

    public final SignBlockEntity sign;
    public final String[] lines;

    private SignChangeEvent(SignBlockEntity sign, String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public static class Pre extends SignChangeEvent {
        public Pre(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }

        @FunctionalInterface
        public interface Listener {
            void onSignChange(Pre event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onSignChange(event);
                if (event.isCanceled()) break;
            }
        });
    }

    public static class Post extends SignChangeEvent {
        public Post(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }

        @FunctionalInterface
        public interface Listener {
            void onSignChange(Post event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onSignChange(event);
            }
        });
    }
}