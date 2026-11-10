package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;


public interface Environment extends li.cil.oc.api.network.Environment, li.cil.oc.api.network.EnvironmentHost {
  default boolean isConnected() {
    Node node = node();
    return node != null && node.address() != null && node.network() != null;
  }

  @SuppressWarnings("unused")
  void initialize();

  @SuppressWarnings("unused")
  void dispose();

  @SuppressWarnings("unused")
  void readFromNBTForServer(net.minecraft.nbt.CompoundTag nbt);

  @SuppressWarnings("unused")
  void writeToNBTForServer(net.minecraft.nbt.CompoundTag nbt);

  @SuppressWarnings("unused")
  void readFromNBTForClient(net.minecraft.nbt.CompoundTag nbt);

  @SuppressWarnings("unused")
  void writeToNBTForClient(net.minecraft.nbt.CompoundTag nbt);

  default Object result(Object... args) {
    return li.cil.oc.core.util.ResultWrapper.result(args);
  }

  default long getLastOperation() {
    return 0;
  }

  @Override
  default void onDisconnect(Node node) {
    flushBuffer(node, node());
  }

  static void flushBuffer(Node node, Node ownNode) {
    if (node == ownNode && node instanceof Connector connector) {
      double bufferSize = connector.localBufferSize();
      connector.setLocalBufferSize(0);
      connector.setLocalBufferSize(bufferSize);
    }
  }
}
