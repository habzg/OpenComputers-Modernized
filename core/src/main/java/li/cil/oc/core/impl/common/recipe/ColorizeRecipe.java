package li.cil.oc.core.impl.common.recipe;

import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ColorizeRecipe extends CustomRecipe {
    public final Item targetItem;
    public final Item[] sourceItems;
    private static RecipeSerializer<?> SERIALIZER;

    public static void setSerializer(RecipeSerializer<?> serializer) {
        SERIALIZER = serializer;
    }

    public ColorizeRecipe(CraftingBookCategory category, Item target, Item[] source) {
        super(category);
        this.targetItem = target;
        this.sourceItems = source != null ? source : new Item[]{target};
    }

    public ColorizeRecipe(Item target, Item[] source) {
        this(CraftingBookCategory.MISC, target, source);
    }

    public ColorizeRecipe(Block target, Item[] source) {
        this(target.asItem(), source);
    }

    @SuppressWarnings("unused")
    public ColorizeRecipe(Block target) {
        this(target, null);
    }

    @Override
    public boolean matches(@NotNull CraftingInput crafting, @NotNull Level world) {
        ItemStack[] stacks = getItems(crafting);
        java.util.List<ItemStack> targets = new java.util.ArrayList<>();
        java.util.List<ItemStack> other = new java.util.ArrayList<>();
        for (var stack : stacks) {
            if (stack == null) continue;
            boolean isTarget = false;
            for (var si : sourceItems) {
                if (stack.getItem() == si) {
                    isTarget = true;
                    break;
                }
            }
            if (isTarget || stack.getItem() == targetItem) {
                targets.add(stack);
            } else {
                other.add(stack);
            }
        }
        if (targets.size() != 1) return false;
        if (other.isEmpty()) return false;
        return other.stream().allMatch(Color::isDye);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput crafting, HolderLookup.@NotNull Provider provider) {
        ItemStack targetStack = null;
        int[] color = new int[]{0, 0, 0};
        int colorCount = 0;
        int maximum = 0;

        ItemStack[] stacks = getItems(crafting);
        for (var stack : stacks) {
            if (stack == null) continue;
            boolean isTarget = false;
            for (var si : sourceItems) {
                if (stack.getItem() == si) {
                    isTarget = true;
                    break;
                }
            }
            if (isTarget || stack.getItem() == targetItem) {
                targetStack = stack.copy();
                targetStack.setCount(1);
            } else {
                var dye = Color.findDye(stack);
                if (dye == null) return ItemStack.EMPTY;

                int dyeColorValue = DyeColor.values()[15 - java.util.Arrays.asList(Color.dyes).indexOf(dye)].getTextColor();
                float[] itemColor = new float[]{
                        ((dyeColorValue >> 16) & 0xFF) / 255.0F,
                        ((dyeColorValue >> 8) & 0xFF) / 255.0F,
                        (dyeColorValue & 0xFF) / 255.0F
                };
                int red = (int) (itemColor[0] * 255.0F);
                int green = (int) (itemColor[1] * 255.0F);
                int blue = (int) (itemColor[2] * 255.0F);
                maximum += Math.max(red, Math.max(green, blue));
                color[0] += red;
                color[1] += green;
                color[2] += blue;
                colorCount++;
            }
        }

        if (targetStack == null) return ItemStack.EMPTY;

        if (targetItem == targetStack.getItem()) {
            if (ItemColorizer.hasColor(targetStack)) {
                int itemColor = ItemColorizer.getColor(targetStack);
                float red = (float) (itemColor >> 16 & 255) / 255.0F;
                float green = (float) (itemColor >> 8 & 255) / 255.0F;
                float blue = (float) (itemColor & 255) / 255.0F;
                maximum = (int) ((float) maximum + Math.max(red, Math.max(green, blue)) * 255.0F);
                color[0] = (int) ((float) color[0] + red * 255.0F);
                color[1] = (int) ((float) color[1] + green * 255.0F);
                color[2] = (int) ((float) color[2] + blue * 255.0F);
                colorCount++;
            }
        } else {
            boolean isSource = false;
            for (var si : sourceItems) {
                if (targetStack.getItem() == si) {
                    isSource = true;
                    break;
                }
            }
            if (isSource) {
                targetStack = new ItemStack(targetItem, targetStack.getCount());
            }
        }

        int red = color[0] / colorCount;
        int green = color[1] / colorCount;
        int blue = color[2] / colorCount;
        float max = (float) maximum / (float) colorCount;
        float div = Math.max(red, Math.max(green, blue));
        if (div > 0) {
            red = (int) ((float) red * max / div);
            green = (int) ((float) green * max / div);
            blue = (int) ((float) blue * max / div);
        }
        ItemColorizer.setColor(targetStack, (red << 16) | (green << 8) | blue);
        return targetStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 10;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private ItemStack[] getItems(CraftingInput crafting) {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (int i = 0; i < crafting.size(); i++) {
            var stack = crafting.getItem(i);
            if (!stack.isEmpty()) list.add(stack);
        }
        return list.toArray(new ItemStack[0]);
    }
}
