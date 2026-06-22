package li.cil.oc.neoforge.util;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.core.util.MachineStateHelper;

public class NeoMachineStateHelper extends MachineStateHelper {
    @Override
    public boolean isInSynchronizedCall(Machine machine) {
        var state = ((li.cil.oc.neoforge.server.machine.Machine) machine).state();
        for (var s : state) {
            if (s.id == STATE_SYNCHRONIZED_CALL || s.id == STATE_SYNCHRONIZED_RETURN) return true;
        }
        return false;
    }
}
