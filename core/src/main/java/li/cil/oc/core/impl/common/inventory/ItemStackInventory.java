package li.cil.oc.core.impl.common.inventory;

import li.cil.oc.core.impl.Settings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;


public interface ItemStackInventory extends Inventory {
    ItemStack container();

    @Override
    default ItemStack[] items() {
        return ((ItemStackInventoryAccessor) this).getItemsArray();
    }

    default void reinitialize(HolderLookup.Provider provider) {
        ItemStack[] items = items();
        for (int i = 0; i < items.length; i++) {
            updateItems(i, null);
        }
        ItemStack c = container();
        if (c != null && !c.isEmpty()) {
            load(dataTag(c), provider);
        }
    }

    default void setChanged(HolderLookup.Provider provider) {
        ItemStack c = container();
        if (c != null && !c.isEmpty()) {
            CompoundTag nbt;
            var customData = c.get(DataComponents.CUSTOM_DATA);
            if (customData == null || customData.isEmpty()) {
                nbt = new CompoundTag();
            } else {
                nbt = customData.copyTag();
            }
            CompoundTag data = nbt.contains(Settings.namespace + "data") ? nbt.getCompound(Settings.namespace + "data") : new CompoundTag();
            save(data, provider);
            nbt.put(Settings.namespace + "data", data);
            c.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    private static CompoundTag dataTag(ItemStack stack) {
        CompoundTag nbt;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            nbt = new CompoundTag();
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        } else {
            nbt = customData.copyTag();
        }
        if (!nbt.contains(Settings.namespace + "data")) {
            nbt.put(Settings.namespace + "data", new CompoundTag());
        }
        return nbt.getCompound(Settings.namespace + "data");
    }

    interface ItemStackInventoryAccessor {
        ItemStack[] getItemsArray();
    }
}
