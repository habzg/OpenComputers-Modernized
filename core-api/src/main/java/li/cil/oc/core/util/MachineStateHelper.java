package li.cil.oc.core.util;

import li.cil.oc.api.machine.Machine;

public abstract class MachineStateHelper {
    public static final int STATE_SYNCHRONIZED_CALL = 5;
    public static final int STATE_SYNCHRONIZED_RETURN = 6;

    private static MachineStateHelper instance;

    public static void setInstance(MachineStateHelper inst) {
        instance = inst;
    }

    public static MachineStateHelper get() {
        return instance;
    }

    public abstract boolean isInSynchronizedCall(Machine machine);
}
