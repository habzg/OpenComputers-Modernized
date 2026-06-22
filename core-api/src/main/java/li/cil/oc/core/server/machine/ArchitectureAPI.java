package li.cil.oc.core.server.machine;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;

import java.util.Map;

public abstract class ArchitectureAPI {
    protected final Machine machine;

    protected ArchitectureAPI(Machine machine) {
        this.machine = machine;
    }

    protected Node node() {
        return machine.node();
    }

    protected Map<String, String> components() {
        return machine.components();
    }
}
