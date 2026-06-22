package li.cil.oc.core.common.block.traits;

public interface StateAware {
    @SuppressWarnings("unused")
    java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState();
}
