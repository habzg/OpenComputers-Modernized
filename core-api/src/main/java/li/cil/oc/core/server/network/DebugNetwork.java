package li.cil.oc.core.server.network;

import li.cil.oc.api.network.Packet;


import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebugNetwork {
    private static final Map<DebugNode, Boolean> cards = Collections.synchronizedMap(new WeakHashMap<>());

    private DebugNetwork() {
    }

    public static void add(DebugNode card) {
        cards.put(card, true);
    }

    public static void remove(DebugNode card) {
        cards.remove(card);
    }

    public static DebugNode getEndpoint(String tunnel) {
        for (DebugNode node : cards.keySet()) {
            if (node.address().equals(tunnel)) return node;
        }
        return null;
    }

    public interface DebugNode {
        String address();

        void receivePacket(Packet packet) ;
    }
}
