package li.cil.oc.core.impl.common.recipe;

import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class DecolorizeRecipe extends CustomRecipe {
    public final Item targetItem;
    private static RecipeSerializer<?> SERIALIZER;

    public static void setSerializer(RecipeSerializer<?> serializer) {
        SERIALIZER = serializer;
    }

    public DecolorizeRecipe(CraftingBookCategory category, Item target) {
        super(category);
        this.targetItem = target;
    }

    public DecolorizeRecipe(Item target) {
        this(CraftingBookCategory.MISC, target);
    }

    @SuppressWarnings("unused")
    public DecolorizeRecipe(Block target) {
        this(target.asItem());
    }

    @Override
    public boolean matches(@NotNull CraftingInput crafting, @NotNull Level world) {
        ItemStack[] stacks = getItems(crafting);
        java.util.List<ItemStack> targets = new java.util.ArrayList<>();
        java.util.List<ItemStack> other = new java.util.ArrayList<>();
        for (var stack : stacks) {
            if (stack == null) continue;
            if (stack.getItem() == targetItem) {
                targets.add(stack);
            } else {
                other.add(stack);
            }
        }
        return targets.size() == 1 && other.size() == 1 && other.getFirst().getItem() == Items.WATER_BUCKET;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput crafting, HolderLookup.@NotNull Provider provider) {
        ItemStack targetStack = null;

        ItemStack[] stacks = getItems(crafting);
        for (var stack : stacks) {
            if (stack == null) continue;
            if (stack.getItem() == targetItem) {
                targetStack = stack.copy();
                targetStack.setCount(1);
            } else if (stack.getItem() != Items.WATER_BUCKET) {
                return ItemStack.EMPTY;
            }
        }

        if (targetStack == null) return ItemStack.EMPTY;

        ItemColorizer.removeColor(targetStack);
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
