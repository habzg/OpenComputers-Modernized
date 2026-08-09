package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class InventoryProviderServer implements InventoryProvider {
    @Override
    public boolean worksWith(ItemStack stack, Player player) {
        return new DriverServer().worksWith(stack);
    }

    @Override
    public Container getInventory(ItemStack stack, Player player) {
        return new ServerInventory() {
            private final ItemStack container = stack;

            @Override
            public ItemStack container() {
                return container;
            }

            @Override
            public void updateItems(int slot, ItemStack stack) {
            }

            @Override
            public boolean stillValid(@NotNull Player player) {
                return true;
            }

            @Override
            public void clearContent() {
            }
        };
    }
}
