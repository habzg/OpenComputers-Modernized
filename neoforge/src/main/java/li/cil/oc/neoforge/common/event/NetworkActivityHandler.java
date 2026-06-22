package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.neoforge.event.NetworkActivityEventImpl;
import li.cil.oc.neoforge.server.component.Server;
import net.neoforged.bus.api.SubscribeEvent;

public final class NetworkActivityHandler {
    private NetworkActivityHandler() {
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onNetworkActivity(NetworkActivityEventImpl.Server e) {
        if (e.tileEntity() instanceof Rack t) {
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                var mountable = t.getMountable(slot);
                if (mountable instanceof Server server) {
                    if (server.componentSlot(e.node().address()) >= 0) {
                        server.lastNetworkActivity = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
            }
        }
    }
}
