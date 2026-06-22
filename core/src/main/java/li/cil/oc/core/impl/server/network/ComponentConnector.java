package li.cil.oc.core.impl.server.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface ComponentConnector extends li.cil.oc.api.network.ComponentConnector, Component, Connector {
    @Override
    default void load(CompoundTag nbt, HolderLookup.Provider provider) {
        Component.super.load(nbt, provider);
        Connector.super.load(nbt, provider);
    }

    @Override
    default void save(CompoundTag nbt, HolderLookup.Provider provider) {
        Component.super.save(nbt, provider);
        Connector.super.save(nbt, provider);
    }

    @Override
    default void onDisconnect(li.cil.oc.api.network.Node node) {
        Connector.super.onDisconnect(node);
    }
}
