package li.cil.oc.neoforge.integration;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

public class ManagedTileEntityEnvironment<T> extends ManagedEnvironment {
    protected final T BlockEntity;

    public ManagedTileEntityEnvironment(final T BlockEntity, final String name) {
        this.BlockEntity = BlockEntity;
        setNode(Network.newNode(this, Visibility.Network).withComponent(name).create());
    }

    protected T getTileEntity() {
        return BlockEntity;
    }
}
