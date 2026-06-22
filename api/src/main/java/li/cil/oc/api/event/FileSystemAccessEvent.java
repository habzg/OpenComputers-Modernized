package li.cil.oc.api.event;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface FileSystemAccessEvent extends Event {
    String sound();

    Level level();

    double x();

    double y();

    double z();

    BlockEntity tileEntity();

    CompoundTag data();

    /**
     * Fired on the server side when a filesystem is accessed. Cancellable.
     */
    interface Server extends FileSystemAccessEvent, CancellableEvent {
        @SuppressWarnings("unused")
        Node node();
    }

    /**
     * Fired on the client side to play filesystem access sounds/effects.
     */
    interface Client extends FileSystemAccessEvent {
    }
}
