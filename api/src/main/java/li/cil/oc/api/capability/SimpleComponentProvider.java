package li.cil.oc.api.capability;

import li.cil.oc.api.network.Node;

/**
 * Implemented on capability providers that wrap block entities implementing
 * {@link li.cil.oc.api.network.SimpleComponent}.
 * <br>
 * This replaces the ASM-based class transformation previously used to inject
 * {@link li.cil.oc.api.network.Environment} methods into SimpleComponent-annotated
 * tile entities. Instead, the capability is attached via
 * <code>AttachCapabilitiesEvent</code> and its node is discovered by OC's network
 * code during {@link li.cil.oc.api.Network#joinOrCreateNetwork}.
 */
public interface SimpleComponentProvider {
    /**
     * Returns the OC network node for the wrapped block entity.
     */
    @SuppressWarnings("unused")
    Node node();

    /**
     * Returns the component name.
     */
    @SuppressWarnings("unused")
    String getComponentName();
}
