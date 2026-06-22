package li.cil.oc.core.server.machine;

import li.cil.oc.api.network.ManagedEnvironment;

public interface EnvironmentHost {
    ManagedEnvironment[] environments();
}
