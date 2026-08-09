package li.cil.oc.core.impl.integration;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

public class ManagedBlockEntityEnvironment<T> extends AbstractManagedEnvironment {
    protected final T BlockEntity;

    public ManagedBlockEntityEnvironment(final T BlockEntity, final String name) {
        this.BlockEntity = BlockEntity;
        setNode(Network.newNode(this, Visibility.Network).withComponent(name).create());
    }

    protected T getBlockEntity() {
        return BlockEntity;
    }
}
