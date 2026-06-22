package li.cil.oc.core.impl.util;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.util.ServerNetwork;


public class NeoServerNetwork extends ServerNetwork {
    @Override
    public void sendFileSystemActivity(Node node, EnvironmentHost host, String name) {
        li.cil.oc.core.impl.common.PacketSender.sendFileSystemActivity(node, host, name);
    }

    @Override
    public void sendNetworkActivity(Node node, EnvironmentHost host) {
        li.cil.oc.core.impl.common.PacketSender.sendNetworkActivity(node, host);
    }
}
