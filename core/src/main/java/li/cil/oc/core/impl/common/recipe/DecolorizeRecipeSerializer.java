package li.cil.oc.core.impl.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class DecolorizeRecipeSerializer implements RecipeSerializer<DecolorizeRecipe> {
    public static final DecolorizeRecipeSerializer INSTANCE = new DecolorizeRecipeSerializer();

    private static final MapCodec<DecolorizeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("target").forGetter(r -> r.targetItem)
            ).apply(instance, DecolorizeRecipeSerializer::create)
    );

    private static DecolorizeRecipe create(CraftingBookCategory category, Item target) {
        return new DecolorizeRecipe(category, target);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, DecolorizeRecipe> STREAM_CODEC = StreamCodec.of(
            DecolorizeRecipeSerializer::toNetwork, DecolorizeRecipeSerializer::fromNetwork
    );

    private DecolorizeRecipeSerializer() {
    }

    @Override
    public @NotNull MapCodec<DecolorizeRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, DecolorizeRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static DecolorizeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        var category = buffer.readEnum(CraftingBookCategory.class);
        var item = BuiltInRegistries.ITEM.byId(buffer.readVarInt());
        return new DecolorizeRecipe(category, item);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, DecolorizeRecipe recipe) {
        buffer.writeEnum(recipe.category());
        buffer.writeVarInt(BuiltInRegistries.ITEM.getId(recipe.targetItem));
    }
}
