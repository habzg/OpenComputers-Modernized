package li.cil.oc.core.util;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;


public abstract class ServerNetwork {
    private static ServerNetwork instance;

    public static void setInstance(ServerNetwork inst) {
        instance = inst;
    }

    public static ServerNetwork get() {
        return instance;
    }

    public abstract void sendFileSystemActivity(Node node, EnvironmentHost host, String name) ;

    public abstract void sendNetworkActivity(Node node, EnvironmentHost host) ;
}
