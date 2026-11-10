package li.cil.oc.core.impl.server.network;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.server.driver.Registry;
import li.cil.oc.core.impl.server.machine.ArgumentsImpl;
import li.cil.oc.core.impl.server.machine.MachineBase;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.server.machine.CallbackWrapper;
import li.cil.oc.core.server.machine.Callbacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface Component extends li.cil.oc.api.network.Component, Node {
  String name();

  Visibility visibility();


  default Map<String, CallbackWrapper> callbacks() {
    return Callbacks.apply(host());
  }

  default void setVisibility(Visibility value) {
    if (value.ordinal() > reachability().ordinal()) {
      throw new IllegalArgumentException("Visibility exceeds reachability on '" + name() + "'");
    }
    if (SideTracker.isServer() && network() != null) {
      Visibility old = visibility();
      if (old == Visibility.Neighbors) {
        if (value == Visibility.Network) addTo(reachableNodes());
        else if (value == Visibility.None) removeFrom(neighbors());
      } else if (old == Visibility.Network) {
        if (value == Visibility.Neighbors) {
          Set<li.cil.oc.api.network.Node> ns = new HashSet<>(neighbors());
          removeFrom(reachableNodes().stream().filter(n -> !ns.contains(n)).collect(java.util.stream.Collectors.toList()));
        } else if (value == Visibility.None) removeFrom(reachableNodes());
      } else if (old == Visibility.None) {
        if (value == Visibility.Neighbors) addTo(neighbors());
        else if (value == Visibility.Network) addTo(reachableNodes());
      }
    }
    if (SideTracker.isServer()) {
      _visibility(value);
    }
  }

  void _visibility(Visibility v);

  default boolean canBeSeenFrom(li.cil.oc.api.network.Node other) {
    return switch (visibility()) {
      case None -> false;
      case Network -> canBeReachedFrom(other);
      case Neighbors -> isNeighborOf(other);
    };
  }

  default void addTo(List<li.cil.oc.api.network.Node> nodes) {
    for (li.cil.oc.api.network.Node n : nodes) {
      if (n.host() instanceof MachineBase) ((MachineBase) n.host()).addComponent(this);
    }
  }

  default void removeFrom(List<li.cil.oc.api.network.Node> nodes) {
    for (li.cil.oc.api.network.Node n : nodes) {
      if (n.host() instanceof MachineBase) ((MachineBase) n.host()).removeComponent(this);
    }
  }

  default Set<String> methods() {
    return callbacks().keySet();
  }

  default Callback annotation(String method) {
    CallbackWrapper cb = callbacks().get(method);
    if (cb == null) throw new RuntimeException(new NoSuchMethodException());
    return cb.annotation();
  }

  default Object[] invoke(String method, Context context, Object... arguments) {
    CallbackWrapper cb = callbacks().get(method);
    if (cb == null) throw new RuntimeException(new NoSuchMethodException());
    Object environment = cb.environment();
    if (environment == null) environment = host();
    return Registry.INSTANCE.convert(cb.apply(environment, context, new ArgumentsImpl(arguments)));
  }

  default void load(CompoundTag nbt, HolderLookup.Provider provider) {
    Node.super.load(nbt, provider);
    if (nbt.contains("visibility")) _visibility(Visibility.values()[nbt.getInt("visibility")]);
  }

  default void save(CompoundTag nbt, HolderLookup.Provider provider) {
    Node.super.save(nbt, provider);
    nbt.putInt("visibility", visibility().ordinal());
  }
}
