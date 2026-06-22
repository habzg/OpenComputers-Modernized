package li.cil.oc.core.impl.common.tileentity.traits;

public interface SwitchLike extends Hub {
    @SuppressWarnings("unused")
    int relayDelay();

    @SuppressWarnings("unused")
    boolean isWirelessEnabled();

    @SuppressWarnings("unused")
    boolean isLinkedEnabled();

    @SuppressWarnings("unused")
    java.util.List<Object> computers();

    @SuppressWarnings("unused")
    java.util.Map<Object, java.util.Set<Integer>> openPorts();

    long lastMessage();

    void lastMessage(long value);

    @SuppressWarnings("unused")
    void onSwitchActivity();
}
