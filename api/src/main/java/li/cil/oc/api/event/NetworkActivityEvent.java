package li.cil.oc.api.event;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface NetworkActivityEvent extends Event {
    /**
     * The Level the network card lives in.
     */
    Level level();

    /**
     * The x coordinate of the network card's container.
     */
    double x();

    /**
     * The y coordinate of the network card's container.
     */
    double y();

    /**
     * The z coordinate of the network card's container.
     */
    double z();

    /**
     * The tile entity hosting the network card.
     * <br>
     * <em>Important</em>: this can be <code>null</code>, which is usually the
     * case when the container is an entity or item.
     */
    BlockEntity tileEntity();

    /**
     * Addition custom data, this is used to transmit the number of the server
     * in a server rack the network card lives in, for example.
     */
    CompoundTag data();

    /**
     * Fired on the server side when a network card signals activity. Cancellable.
     */
    interface Server extends NetworkActivityEvent, CancellableEvent {
        /**
         * The node of the network card that signalled activity.
         */
        @SuppressWarnings("unused")
        Node node();
    }

    /**
     * Fired on the client side to play network activity effects.
     */
    interface Client extends NetworkActivityEvent {
    }
}
