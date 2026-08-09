package li.cil.oc.fabric.common.event;

import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.core.impl.server.component.Server;

public final class NetworkActivityHandler {
    @SuppressWarnings("unused")
    private NetworkActivityHandler() {
    }

    public static void onNetworkActivityServer(NetworkActivityEvent.Server e) {
        if (e.getBlockEntity() instanceof Rack t) {
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                var mountable = t.getMountable(slot);
                if (mountable instanceof Server server) {
                    if (server.componentSlot(e.getNode().address()) >= 0) {
                        server.lastNetworkActivity = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
            }
        }
    }
}
