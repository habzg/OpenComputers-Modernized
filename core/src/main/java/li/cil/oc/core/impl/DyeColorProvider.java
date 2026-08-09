package li.cil.oc.core.impl;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DyeColorProvider implements IDyeColorProvider {
    public static final IDyeColorProvider INSTANCE = new DyeColorProvider();

    private static final String[] DYE_NAMES = {
            "dyeBlack", "dyeRed", "dyeGreen", "dyeBrown",
            "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray",
            "dyeGray", "dyePink", "dyeLime", "dyeYellow",
            "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"
    };

    private DyeColorProvider() {}

    @Override
    public String findDye(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Holder<Item> itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        for (DyeColor dye : DyeColor.values()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dyes/" + dye.getName()));
            if (itemHolder.is(tag)) {
                return DYE_NAMES[15 - dye.getId()];
            }
        }
        if (stack.getItem() instanceof DyeItem dyeItem) {
            DyeColor dyeColor = dyeItem.getDyeColor();
            return DYE_NAMES[15 - dyeColor.getId()];
        }
        return null;
    }
}
