package li.cil.oc.fabric.integration.vanilla;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.api.driver.InventoryProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class DriverItemHandler implements InventoryProvider {
    @Override
    public boolean worksWith(ItemStack stack, Player player) {
        return ItemStorage.ITEM.find(stack, null) != null;
    }

    @Override
    public Container getInventory(ItemStack stack, Player player) {
        Storage<ItemVariant> storage = ItemStorage.ITEM.find(stack, null);
        return storage == null ? null : new ItemStorageContainer(storage);
    }

    private record ItemStorageContainer(Storage<ItemVariant> storage) implements Container {

        private List<StorageView<ItemVariant>> snapshotViews() {
                var views = new ArrayList<StorageView<ItemVariant>>();
                for (var view : storage) {
                    if (!view.isResourceBlank() || view.getAmount() > 0) {
                        views.add(view);
                    }
                }
                return views;
            }

            @Override
            public int getContainerSize() {
                return Math.max(1, snapshotViews().size());
            }

            @Override
            public boolean isEmpty() {
                return StorageUtil.simulateExtract(storage, ItemVariant.blank(), Long.MAX_VALUE, null) == 0;
            }

            @Override
            public @NotNull ItemStack getItem(int slot) {
                var views = snapshotViews();
                if (slot < 0 || slot >= views.size()) return ItemStack.EMPTY;
                var view = views.get(slot);
                return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
            }

            @Override
            public @NotNull ItemStack removeItem(int slot, int count) {
                var views = snapshotViews();
                if (slot < 0 || slot >= views.size()) return ItemStack.EMPTY;
                var view = views.get(slot);
                var variant = view.getResource();
                long extracted;
                try (Transaction tx = Transaction.openOuter()) {
                    extracted = storage.extract(variant, count, tx);
                    tx.commit();
                }
                return extracted > 0 ? variant.toStack((int) extracted) : ItemStack.EMPTY;
            }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, getItem(slot).getCount());
        }

            @Override
            public void setItem(int slot, @NotNull ItemStack stack) {
                var views = snapshotViews();
                if (slot >= 0 && slot < views.size()) {
                    var view = views.get(slot);
                    try (Transaction tx = Transaction.openOuter()) {
                        storage.extract(view.getResource(), view.getAmount(), tx);
                        tx.commit();
                    }
                }
                if (!stack.isEmpty()) {
                    try (Transaction tx = Transaction.openOuter()) {
                        storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
                        tx.commit();
                    }
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
                try (Transaction tx = Transaction.openOuter()) {
                    for (var view : storage) {
                        storage.extract(view.getResource(), view.getAmount(), tx);
                    }
                    tx.commit();
                }
            }
        }
}
