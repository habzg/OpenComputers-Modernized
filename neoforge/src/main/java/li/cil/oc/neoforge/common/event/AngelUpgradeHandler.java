package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.UpgradeAngel;
import li.cil.oc.neoforge.event.RobotPlaceInAirEventImpl;
import net.neoforged.bus.api.SubscribeEvent;

public final class AngelUpgradeHandler {
    @SubscribeEvent
    public static void onPlaceInAir(RobotPlaceInAirEventImpl e) {
        Node machineNode = e.agent().machine().node();
        boolean allowed = false;
        for (Node node : machineNode.reachableNodes()) {
            if (node.canBeReachedFrom(machineNode) && node.host() instanceof UpgradeAngel) {
                allowed = true;
                break;
            }
        }
        e.setAllowed(allowed);
    }
}
