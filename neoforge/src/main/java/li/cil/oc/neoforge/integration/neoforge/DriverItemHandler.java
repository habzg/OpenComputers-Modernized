package li.cil.oc.neoforge.integration.neoforge;

import li.cil.oc.api.driver.InventoryProvider;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public final class DriverItemHandler implements InventoryProvider {
    @Override
    public boolean worksWith(ItemStack stack, Player ignoredPlayer) {
        return stack.getCapability(Capabilities.ItemHandler.ITEM) != null;
    }

    @Override
    public Container getInventory(ItemStack stack, Player ignoredPlayer) {
        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        return handler == null ? null : new ItemHandlerContainer(handler);
    }

    private record ItemHandlerContainer(IItemHandler handler) implements Container {

        @Override
            public int getContainerSize() {
                return handler.getSlots();
            }

            @Override
            public boolean isEmpty() {
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (!handler.getStackInSlot(i).isEmpty()) return false;
                }
                return true;
            }

            @Override
            public @NotNull ItemStack getItem(int slot) {
                return handler.getStackInSlot(slot);
            }

            @Override
            public @NotNull ItemStack removeItem(int slot, int count) {
                return handler.extractItem(slot, count, false);
            }

            @Override
            public @NotNull ItemStack removeItemNoUpdate(int slot) {
                return handler.extractItem(slot, handler.getStackInSlot(slot).getCount(), false);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                handler.extractItem(slot, handler.getStackInSlot(slot).getCount(), false);
                if (!stack.isEmpty()) {
                    handler.insertItem(slot, stack, false);
                }
            }

            @Override
            public void setChanged() {
            }

            @Override
            public boolean stillValid(@NotNull Player player) {
                return true;
            }

            @Override
            public void clearContent() {
                for (int i = 0; i < handler.getSlots(); i++) {
                    handler.extractItem(i, handler.getStackInSlot(i).getCount(), false);
                }
            }
        }
}
