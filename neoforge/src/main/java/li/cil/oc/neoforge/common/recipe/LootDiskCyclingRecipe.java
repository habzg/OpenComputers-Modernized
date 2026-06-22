package li.cil.oc.neoforge.common.recipe;

import com.mojang.serialization.MapCodec;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.neoforge.common.Loot;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LootDiskCyclingRecipe extends CustomRecipe {
    @SuppressWarnings("unused")
    public LootDiskCyclingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@NotNull CraftingInput crafting, @NotNull Level world) {
        ItemStack[] stacks = collectStacks(crafting);
        boolean hasLoot = false;
        boolean hasWrench = false;
        for (var stack : stacks) {
            if (Loot.isLootDisk(stack)) hasLoot = true;
            if (Wrench.isWrench(stack)) hasWrench = true;
        }
        return stacks.length == 2 && hasLoot && hasWrench;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput crafting, HolderLookup.@NotNull Provider provider) {
        var lootDiskStacks = Loot.disksForCycling();
        ItemStack[] stacks = collectStacks(crafting);
        ItemStack lootDisk = null;
        for (var stack : stacks) {
            if (Loot.isLootDisk(stack)) {
                lootDisk = stack;
                break;
            }
        }
        if (lootDisk != null && !lootDiskStacks.isEmpty()) {
            String lootFactoryName = getLootFactoryName(lootDisk);
            int oldIndex = -1;
            for (int i = 0; i < lootDiskStacks.size(); i++) {
                if (getLootFactoryName(lootDiskStacks.get(i)).equals(lootFactoryName)) {
                    oldIndex = i;
                    break;
                }
            }
            int newIndex = (oldIndex + 1) % lootDiskStacks.size();
            return lootDiskStacks.get(newIndex).copy();
        }
        return ItemStack.EMPTY;
    }

    public String getLootFactoryName(ItemStack stack) {
        CustomData _ld = stack.get(DataComponents.CUSTOM_DATA);
        return _ld != null ? _ld.copyTag().getString(Settings.namespace + "lootFactory") : "";
    }

    public ItemStack[] collectStacks(CraftingInput crafting) {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (int i = 0; i < crafting.size(); i++) {
            var stack = crafting.getItem(i);
            if (!stack.isEmpty()) list.add(stack);
        }
        return list.toArray(new ItemStack[0]);
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.<Ingredient>create();
        var disks = Loot.disksForCycling();
        if (!disks.isEmpty()) {
            list.add(Ingredient.of(disks.toArray(new ItemStack[0])));
        } else {
            var floppy = Items.get(Constants.ItemName.Floppy);
            if (floppy != null) list.add(Ingredient.of(floppy.item()));
        }
        var wrench = Items.get(Constants.ItemName.Wrench);
        if (wrench != null) list.add(Ingredient.of(wrench.item()));
        return list;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        var disks = Loot.disksForCycling();
        if (!disks.isEmpty()) return disks.getFirst().copy();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @SuppressWarnings("unused")
    public static class Serializer implements RecipeSerializer<LootDiskCyclingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<LootDiskCyclingRecipe> CODEC =
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                        .xmap(LootDiskCyclingRecipe::new, LootDiskCyclingRecipe::category);

        private static final StreamCodec<RegistryFriendlyByteBuf, LootDiskCyclingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        CraftingBookCategory.STREAM_CODEC, LootDiskCyclingRecipe::category,
                        LootDiskCyclingRecipe::new
                );

        @Override
        public @NotNull MapCodec<LootDiskCyclingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, LootDiskCyclingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
