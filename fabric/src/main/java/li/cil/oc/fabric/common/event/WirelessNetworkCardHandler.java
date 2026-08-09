package li.cil.oc.fabric.common.event;

import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.WirelessNetworkCard;

public final class WirelessNetworkCardHandler {
    public static void onMove(RobotMoveEvent.Post e) {
        Node machineNode = e.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof WirelessNetworkCard card) {
                li.cil.oc.api.Network.updateWirelessNetwork(card);
            }
        }
    }
}
