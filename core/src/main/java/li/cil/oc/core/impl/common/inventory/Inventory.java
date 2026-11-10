package li.cil.oc.core.impl.common.inventory;

import java.util.Locale;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Inventory extends SimpleInventory {
  static ItemStack[] filledItems(int size) {
    var items = new ItemStack[size];
    java.util.Arrays.fill(items, ItemStack.EMPTY);
    return items;
  }

  ItemStack[] items();

  void updateItems(int slot, ItemStack stack);

  @Override
  default @NotNull ItemStack getItem(int slot) {
    if (slot >= 0 && slot < getContainerSize()) {
      ItemStack[] items = items();
      if (slot < items.length && items[slot] != null) {
        return items[slot];
      }
    }
    return ItemStack.EMPTY;
  }

  @Override
  default void setItem(int slot, @NotNull ItemStack stack) {
    if (slot >= 0 && slot < getContainerSize()) {
      ItemStack current = items()[slot];
      if (current == null) current = ItemStack.EMPTY;
      if (stack.isEmpty() && current.isEmpty()) {
        return;
      }
      if (ItemStack.matches(current, stack)) {
        return;
      }

      ItemStack oldStack = current;
      updateItems(slot, ItemStack.EMPTY);
      if (!oldStack.isEmpty()) {
        onItemRemoved(slot, oldStack);
      }
      if (!stack.isEmpty() && stack.getCount() >= getInventoryStackRequired()) {
        if (stack.getCount() > getMaxStackSize()) {
          stack.setCount(getMaxStackSize());
        }
        updateItems(slot, stack);
      }

      ItemStack added = items()[slot];
      if (added != null && !added.isEmpty()) {
        onItemAdded(slot, added);
      }

      setChanged();
    }
  }

  default String getInventoryName() {
    return "container.opencomputers." + inventoryName().toLowerCase(Locale.ROOT);
  }

  default String inventoryName() {
    return getClass().getSimpleName();
  }

  default void load(CompoundTag nbt, HolderLookup.Provider provider) {
    int count = 0;
    ListTag tagList = nbt.getList(OCSettings.namespace + "items", Tag.TAG_COMPOUND);
    for (int i = 0; i < tagList.size(); i++) {
      CompoundTag tag = tagList.getCompound(i);
      if (tag.contains("slot")) {
        int slot = tag.getByte("slot");
        if (slot >= 0 && slot < items().length) {
          updateItems(slot, ItemStack.parseOptional(provider, tag.getCompound("item")));
        }
      } else {
        if (count >= 0 && count < items().length) {
          updateItems(count, ItemStack.parseOptional(provider, tag));
        }
      }
      count++;
    }
  }

  default void save(CompoundTag nbt, HolderLookup.Provider provider) {
    ListTag list = new ListTag();
    ItemStack[] items = items();
    for (int i = 0; i < items.length; i++) {
      if (items[i] != null && !items[i].isEmpty()) {
        CompoundTag slotNbt = new CompoundTag();
        slotNbt.putByte("slot", (byte) i);
        slotNbt.put("item", items[i].save(provider));
        list.add(slotNbt);
      }
    }
    nbt.put(OCSettings.namespace + "items", list);
  }

  default void onItemAdded(int slot, ItemStack stack) {
  }

  default void onItemRemoved(int slot, ItemStack stack) {
  }
}
