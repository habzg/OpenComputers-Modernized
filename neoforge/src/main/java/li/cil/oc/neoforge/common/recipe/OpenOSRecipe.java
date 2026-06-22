package li.cil.oc.neoforge.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

import java.util.ArrayList;
import java.util.List;

public class OpenOSRecipe extends CustomRecipe {
    private final List<Ingredient> ingredients;

    @SuppressWarnings("unused")
    public OpenOSRecipe(CraftingBookCategory category, List<Ingredient> ingredients) {
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
        var info = Items.get(Constants.ItemName.OpenOS);
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
        var info = Items.get(Constants.ItemName.OpenOS);
        return info != null ? info.createItemStack(1) : ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return OpenOSRecipeSerializer.INSTANCE;
    }

    @SuppressWarnings("unused")
    public static class OpenOSRecipeSerializer implements RecipeSerializer<OpenOSRecipe> {
        public static final OpenOSRecipeSerializer INSTANCE = new OpenOSRecipeSerializer();

        private static final MapCodec<OpenOSRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                        Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients)
                ).apply(instance, OpenOSRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, OpenOSRecipe> STREAM_CODEC = StreamCodec.of(
                OpenOSRecipeSerializer::toNetwork, OpenOSRecipeSerializer::fromNetwork
        );

        @Override
        public @NotNull MapCodec<OpenOSRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, OpenOSRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static OpenOSRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            var category = buffer.readEnum(CraftingBookCategory.class);
            int count = buffer.readVarInt();
            var ingredients = new ArrayList<Ingredient>(count);
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            return new OpenOSRecipe(category, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, OpenOSRecipe recipe) {
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.ingredients.size());
            for (var ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
        }
    }
}
