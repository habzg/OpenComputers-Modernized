package li.cil.oc.core.impl.server.network;

import com.google.common.base.Strings;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

public interface Node extends li.cil.oc.api.network.Node {
    Logger LOGGER = LoggerFactory.getLogger(Node.class);

    Environment host();

    Visibility reachability();

    String address();

    void address_$eq(String value);

    li.cil.oc.api.network.Network network();

    void network_$eq(li.cil.oc.api.network.Network value);

    default boolean canBeReachedFrom(li.cil.oc.api.network.Node other) {
        return switch (reachability()) {
            case None -> false;
            case Neighbors -> isNeighborOf(other);
            case Network -> isInSameNetwork(other);
        };
    }

    default boolean isNeighborOf(li.cil.oc.api.network.Node other) {
        return isInSameNetwork(other) && java.util.stream.StreamSupport.stream(network().neighbors(this).spliterator(), false).anyMatch(n -> n == other);
    }

    default List<li.cil.oc.api.network.Node> reachableNodes() {
        if (network() == null) return new ArrayList<>();
        List<li.cil.oc.api.network.Node> result = new ArrayList<>();
        network().nodes(this).forEach(result::add);
        return result;
    }

    default List<li.cil.oc.api.network.Node> neighbors() {
        if (network() == null) return new ArrayList<>();
        List<li.cil.oc.api.network.Node> result = new ArrayList<>();
        network().neighbors(this).forEach(result::add);
        return result;
    }

    default void connect(li.cil.oc.api.network.Node node) {
        if (network() != null) network().connect(this, node);
    }

    default void disconnect(li.cil.oc.api.network.Node node) {
        if (network() != null && isInSameNetwork(node)) network().disconnect(this, node);
    }

    default void remove() {
        if (network() != null) network().remove(this);
    }

    default boolean isInSameNetwork(li.cil.oc.api.network.Node other) {
        return network() != null && other != null && network() == other.network();
    }

    default void onConnect(li.cil.oc.api.network.Node node) {
        try {
            host().onConnect(node);
        } catch (Throwable e) {
            LOGGER.warn("Component '{}' threw error on connect.", host().getClass().getName(), e);
        }
    }

    default void onDisconnect(li.cil.oc.api.network.Node node) {
        try {
            host().onDisconnect(node);
        } catch (Throwable e) {
            LOGGER.warn("Component '{}' threw error on disconnect.", host().getClass().getName(), e);
        }
    }

    default void load(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.contains("address")) {
            String newAddress = nbt.getString("address");
            if (!Strings.isNullOrEmpty(newAddress) && !newAddress.equals(address())) {
                if (network() instanceof Network.Wrapper) ((Network.Wrapper) network()).network.remap(this, newAddress);
                else address_$eq(newAddress);
            }
        }
    }

    default void save(CompoundTag nbt, HolderLookup.Provider provider) {
        if (address() != null) nbt.putString("address", address());
    }

    default void sendToAddress(String target, String name, Object... data) {
        if (network() != null) network().sendToAddress(this, target, name, data);
    }

    default void sendToNeighbors(String name, Object... data) {
        if (network() != null) network().sendToNeighbors(this, name, data);
    }

    default void sendToReachable(String name, Object... data) {
        if (network() != null) network().sendToReachable(this, name, data);
    }

    default void sendToVisible(String name, Object... data) {
        if (network() != null) network().sendToVisible(this, name, data);
    }

}
