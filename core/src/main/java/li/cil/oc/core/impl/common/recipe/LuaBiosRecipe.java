package li.cil.oc.core.impl.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LuaBiosRecipe extends CustomRecipe {
    private final List<Ingredient> ingredients;

    @SuppressWarnings("unused")
    public LuaBiosRecipe(CraftingBookCategory category, List<Ingredient> ingredients) {
        super(category);
        this.ingredients = ingredients;
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        var remaining = new ArrayList<>(ingredients);
        outer:
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (!stack.isEmpty()) {
                for (var it = remaining.iterator(); it.hasNext(); ) {
                    if (it.next().test(stack)) {
                        it.remove();
                        continue outer;
                    }
                }
                return false;
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider provider) {
        var info = Items.get(Constants.ItemName.LuaBios);
        if (info != null) {
            return info.createItemStack(1);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.<Ingredient>createWithCapacity(ingredients.size());
        list.addAll(ingredients);
        return list;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        var info = Items.get(Constants.ItemName.LuaBios);
        return info != null ? info.createItemStack(1) : ItemStack.EMPTY;
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
        return LuaBiosRecipeSerializer.INSTANCE;
    }

    @SuppressWarnings("unused")
    public static class LuaBiosRecipeSerializer implements RecipeSerializer<LuaBiosRecipe> {
        public static final LuaBiosRecipeSerializer INSTANCE = new LuaBiosRecipeSerializer();

        private static final MapCodec<LuaBiosRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                        Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients)
                ).apply(instance, LuaBiosRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, LuaBiosRecipe> STREAM_CODEC = StreamCodec.of(
                LuaBiosRecipeSerializer::toNetwork, LuaBiosRecipeSerializer::fromNetwork
        );

        @Override
        public @NotNull MapCodec<LuaBiosRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, LuaBiosRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static LuaBiosRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            var category = buffer.readEnum(CraftingBookCategory.class);
            int count = buffer.readVarInt();
            var ingredients = new ArrayList<Ingredient>(count);
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            return new LuaBiosRecipe(category, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, LuaBiosRecipe recipe) {
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.ingredients.size());
            for (var ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
        }
    }
}
