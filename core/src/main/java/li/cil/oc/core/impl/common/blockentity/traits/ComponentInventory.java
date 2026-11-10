package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


public interface ComponentInventory extends Environment, Inventory, li.cil.oc.core.impl.common.inventory.ComponentInventory {
  default ItemStack[] pendingRemovals() {
    if (this instanceof BlockEntity be) return be.pendingRemovals(getContainerSize());
    return null;
  }

  default ItemStack[] pendingAdds() {
    if (this instanceof BlockEntity be) return be.pendingAdds(getContainerSize());
    return null;
  }

  void readFromNBTForServer(CompoundTag nbt);

  void writeToNBTForServer(CompoundTag nbt);

  void readFromNBTForClient(CompoundTag nbt);

  void writeToNBTForClient(CompoundTag nbt);

  void onConnect(Node node);

  default void onDisconnect(Node node) {
    Environment.super.onDisconnect(node);
    if (node == node()) {
      disconnectComponents();
    }
  }

  void initialize();

  void dispose();
}
