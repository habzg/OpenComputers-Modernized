package li.cil.oc.core.impl.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

public final class ItemColorizer {

    public static boolean hasColor(ItemStack stack) {
        if (stack.has(DataComponents.DYED_COLOR)) return true;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("display")) {
                return tag.getCompound("display").contains("color");
            }
        }
        return false;
    }

    public static int getColor(ItemStack stack) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed != null) return dyed.rgb();
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : null;
        if (tag != null) {
            CompoundTag displayTag = tag.getCompound("display");
            if (displayTag.isEmpty()) return -1;
            if (displayTag.contains("color")) return displayTag.getInt("color");
        }
        return -1;
    }

    public static void removeColor(ItemStack stack) {
        stack.remove(DataComponents.DYED_COLOR);
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            CompoundTag displayTag = tag.getCompound("display");
            if (displayTag.contains("color")) displayTag.remove("color");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void setColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, false));
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null && !cd.isEmpty() ? cd.copyTag() : new CompoundTag();
        CompoundTag displayTag = tag.getCompound("display");
        if (!tag.contains("display")) {
            tag.put("display", displayTag);
        }
        displayTag.putInt("color", color);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
