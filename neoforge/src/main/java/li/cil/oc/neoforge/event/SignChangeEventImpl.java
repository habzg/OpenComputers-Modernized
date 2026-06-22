package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.SignChangeEvent;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class SignChangeEventImpl extends Event implements SignChangeEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final SignBlockEntity sign;
    protected final String[] lines;

    public SignChangeEventImpl(SignBlockEntity sign, String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }

    @Override
    public SignBlockEntity sign() {
        return sign;
    }

    @Override
    public String[] lines() {
        return lines;
    }

    public static class Pre extends SignChangeEventImpl implements SignChangeEvent.Pre {
        public Pre(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }
    }

    public static class Post extends SignChangeEventImpl implements SignChangeEvent.Post {
        public Post(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }
    }
}
