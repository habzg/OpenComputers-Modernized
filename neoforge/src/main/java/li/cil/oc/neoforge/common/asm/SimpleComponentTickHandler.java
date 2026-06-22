package li.cil.oc.neoforge.common.asm;

import li.cil.oc.api.Network;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public final class SimpleComponentTickHandler {
    public static final ArrayList<Runnable> pending = new java.util.ArrayList<>();
    public static final SimpleComponentTickHandler Instance = new SimpleComponentTickHandler();
    private static final Logger log = LoggerFactory.getLogger("OpenComputers");

    private SimpleComponentTickHandler() {
    }

    public static void schedule(final BlockEntity tileEntity) {
        if (SideTracker.isServer()) {
            synchronized (pending) {
                pending.add(() -> Network.joinOrCreateNetwork(tileEntity));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onTick(ServerTickEvent.Post e) {
        final Runnable[] adds;
        synchronized (pending) {
            adds = pending.toArray(new Runnable[0]);
            pending.clear();
        }
        for (Runnable runnable : adds) {
            try {
                runnable.run();
            } catch (Throwable t) {
                log.warn("Error in scheduled tick action.", t);
            }
        }
    }
}
