package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.WirelessNetworkCard;
import li.cil.oc.neoforge.event.RobotMoveEventImpl;
import net.neoforged.bus.api.SubscribeEvent;

public final class WirelessNetworkCardHandler {
    @SubscribeEvent
    public static void onMove(RobotMoveEventImpl.Post e) {
        Node machineNode = e.agent().machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof WirelessNetworkCard card) {
                li.cil.oc.api.Network.updateWirelessNetwork(card);
            }
        }
    }
}
