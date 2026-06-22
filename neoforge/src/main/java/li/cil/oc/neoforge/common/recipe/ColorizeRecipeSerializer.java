package li.cil.oc.neoforge.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.core.impl.common.recipe.ColorizeRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class ColorizeRecipeSerializer implements RecipeSerializer<ColorizeRecipe> {
    public static final ColorizeRecipeSerializer INSTANCE = new ColorizeRecipeSerializer();

    private static final MapCodec<ColorizeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("target").forGetter(r -> r.targetItem)
            ).apply(instance, ColorizeRecipeSerializer::create)
    );

    private static ColorizeRecipe create(CraftingBookCategory category, Item target) {
        return new ColorizeRecipe(category, target, null);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, ColorizeRecipe> STREAM_CODEC = StreamCodec.of(
            ColorizeRecipeSerializer::toNetwork, ColorizeRecipeSerializer::fromNetwork
    );

    private ColorizeRecipeSerializer() {
    }

    @Override
    public @NotNull MapCodec<ColorizeRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, ColorizeRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static ColorizeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        var category = buffer.readEnum(CraftingBookCategory.class);
        var item = BuiltInRegistries.ITEM.byId(buffer.readVarInt());
        return new ColorizeRecipe(category, item, null);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, ColorizeRecipe recipe) {
        buffer.writeEnum(recipe.category());
        buffer.writeVarInt(BuiltInRegistries.ITEM.getId(recipe.targetItem));
    }
}
