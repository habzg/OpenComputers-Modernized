package li.cil.oc.core.impl.common.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BlockTagIngredient {
    private BlockTagIngredient() {
    }

    public static final ResourceLocation TYPE_ID = ResourceLocation.fromNamespaceAndPath("opencomputers", "block_tag");

    private static final String MARKER_PREFIX = "opencomputers:block_tag:";

    public static final Codec<Ingredient> CODEC = new Codec<>() {
        @Override
        public <T> @NotNull DataResult<Pair<Ingredient, T>> decode(DynamicOps<T> ops, T input) {
            var mapResult = ops.getMap(input);
            if (mapResult.result().isPresent()) {
                var map = mapResult.result().get();
                var typeValue = map.get("type");
                if (typeValue != null && TYPE_ID.toString().equals(ops.getStringValue(typeValue).result().orElse(""))) {
                    return TagKey.codec(Registries.BLOCK).fieldOf("tag").codec()
                            .decode(ops, input)
                            .map(pair -> Pair.of(marker(pair.getFirst()), input));
                }
            }
            return Ingredient.CODEC_NONEMPTY.decode(ops, input);
        }

        @Override
        public <T> @NotNull DataResult<T> encode(Ingredient input, DynamicOps<T> ops, T prefix) {
            return Ingredient.CODEC_NONEMPTY.encode(input, ops, prefix);
        }
    };

    private static Ingredient marker(TagKey<Block> tag) {
        ItemStack marker = new ItemStack(Blocks.BARRIER);
        marker.set(DataComponents.CUSTOM_NAME, Component.literal(MARKER_PREFIX + tag.location()));
        return Ingredient.of(marker);
    }

    @Nullable
    public static TagKey<Block> markerTag(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length != 1 || !items[0].is(Items.BARRIER)) return null;
        Component name = items[0].get(DataComponents.CUSTOM_NAME);
        if (name == null || !name.getString().startsWith(MARKER_PREFIX)) return null;
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(name.getString().substring(MARKER_PREFIX.length())));
    }

    @Nullable
    public static Ingredient expand(TagKey<Block> tag) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
            ItemStack stack = new ItemStack(holder.value());
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks.isEmpty() ? null : Ingredient.of(stacks.stream());
    }
}
