package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.neoforge.common.inventory.DatabaseInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class InventoryProviderDatabase implements InventoryProvider {
    @Override
    public boolean worksWith(ItemStack stack, Player player) {
        return new DriverUpgradeDatabase().worksWith(stack);
    }

    @Override
    public Container getInventory(ItemStack stack, Player player) {
        return new DatabaseInventory() {
            private final ItemStack container = stack;

            @Override
            public ItemStack container() {
                return container;
            }

            @SuppressWarnings("SameReturnValue")
            public boolean isUseableByPlayer(Player player) {
                return true;
            }

            @Override
            public void updateItems(int slot, ItemStack stack) {
            }

            @Override
            public void clearContent() {
            }
        };
    }
}
