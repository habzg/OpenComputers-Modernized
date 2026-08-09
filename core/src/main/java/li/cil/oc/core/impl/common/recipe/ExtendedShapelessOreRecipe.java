package li.cil.oc.core.impl.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ExtendedShapelessOreRecipe extends ShapelessRecipe {
    private final List<BlockTagSlot> blockTagSlots;

    public ExtendedShapelessOreRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
        this.blockTagSlots = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            TagKey<Block> tag = BlockTagIngredient.markerTag(ingredients.get(i));
            if (tag != null) {
                ingredients.set(i, Ingredient.EMPTY);
                blockTagSlots.add(new BlockTagSlot(i, tag));
            }
        }
    }

    private record BlockTagSlot(int index, TagKey<Block> tag) {
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        expandBlockTags();
        return super.getIngredients();
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        expandBlockTags();
        return super.matches(input, level);
    }

    private void expandBlockTags() {
        if (blockTagSlots.isEmpty()) return;
        for (int i = blockTagSlots.size() - 1; i >= 0; i--) {
            Ingredient expanded = BlockTagIngredient.expand(blockTagSlots.get(i).tag());
            if (expanded != null) {
                super.getIngredients().set(blockTagSlots.get(i).index(), expanded);
                blockTagSlots.remove(i);
            }
        }
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput inventory, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
        return ExtendedRecipe.addNBTToResult(this, super.assemble(inventory, provider), inventory, provider);
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput inventory) {
        return ExtendedRecipe.getRecraftRemainingItems(inventory, super.getRemainingItems(inventory));
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<ExtendedShapelessOreRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<ExtendedShapelessOreRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.getResultItem(null)),
                                BlockTagIngredient.CODEC
                                        .listOf()
                                        .fieldOf("ingredients")
                                        .flatXmap(
                                                items -> {
                                                    Ingredient[] aingredient = items.toArray(Ingredient[]::new);
                                                    if (aingredient.length == 0) {
                                                        return DataResult.error(() -> "No ingredients for shapeless recipe");
                                                    } else {
                                                        return aingredient.length > 9
                                                                ? DataResult.error(() -> "Too many ingredients for shapeless recipe. The maximum is: %s".formatted(9))
                                                                : DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
                                                    }
                                                },
                                                DataResult::success
                                        )
                                        .forGetter(ShapelessRecipe::getIngredients)
                        )
                        .apply(instance, ExtendedShapelessOreRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, ExtendedShapelessOreRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork
        );

        @Override
        public @NotNull MapCodec<ExtendedShapelessOreRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ExtendedShapelessOreRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ExtendedShapelessOreRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String s = buffer.readUtf();
            CraftingBookCategory craftingbookcategory = buffer.readEnum(CraftingBookCategory.class);
            int i = buffer.readVarInt();
            NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);
            nonnulllist.replaceAll(p_319735_ -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            ItemStack itemstack = ItemStack.STREAM_CODEC.decode(buffer);
            return new ExtendedShapelessOreRecipe(s, craftingbookcategory, itemstack, nonnulllist);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, ExtendedShapelessOreRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.getIngredients().size());

            for (Ingredient ingredient : recipe.getIngredients()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
        }
    }
}
