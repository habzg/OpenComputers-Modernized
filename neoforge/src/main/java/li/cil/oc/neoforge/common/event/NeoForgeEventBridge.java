package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.event.Event;
import li.cil.oc.api.event.OCEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeEventBridge implements OCEventBus.Bridge {

    private static NeoForgeEventBridge instance;

    private NeoForgeEventBridge() {
    }

    public static void init() {
        if (instance == null) {
            instance = new NeoForgeEventBridge();
            OCEventBus.setBridge(instance);
        }
    }

    @Override
    public <T extends Event> T forward(T event) {
        if (event instanceof net.neoforged.bus.api.Event neoEvent) {
            NeoForge.EVENT_BUS.post(neoEvent);
        }
        return event;
    }
}
