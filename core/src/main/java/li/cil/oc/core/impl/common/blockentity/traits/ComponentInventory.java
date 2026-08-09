package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


public interface ComponentInventory extends Environment, Inventory, li.cil.oc.core.impl.common.inventory.ComponentInventory {
    ItemStack[] pendingRemovals();

    ItemStack[] pendingAdds();

    void readFromNBTForServer(CompoundTag nbt) ;

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
