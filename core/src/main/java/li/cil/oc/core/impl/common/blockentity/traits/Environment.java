package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;


public interface Environment extends li.cil.oc.api.network.Environment, li.cil.oc.api.network.EnvironmentHost {
    @SuppressWarnings("unused")
    boolean isConnected();

    @SuppressWarnings("unused")
    void initialize();

    @SuppressWarnings("unused")
    void dispose();

    @SuppressWarnings("unused")
    void readFromNBTForServer(net.minecraft.nbt.CompoundTag nbt) ;

    @SuppressWarnings("unused")
    void writeToNBTForServer(net.minecraft.nbt.CompoundTag nbt);

    @SuppressWarnings("unused")
    void readFromNBTForClient(net.minecraft.nbt.CompoundTag nbt);

    @SuppressWarnings("unused")
    void writeToNBTForClient(net.minecraft.nbt.CompoundTag nbt);

    @SuppressWarnings("unused")
    Object result(Object... args);

    default long getLastOperation() {
        return 0;
    }

    @Override
    default void onDisconnect(Node node) {
        if (node == node() && node instanceof Connector connector) {
            double bufferSize = connector.localBufferSize();
            connector.setLocalBufferSize(0);
            connector.setLocalBufferSize(bufferSize);
        }
    }
}
