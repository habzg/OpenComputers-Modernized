package li.cil.oc.core.impl.common.block;

import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class ChameliumBlock extends SimpleBlock {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public ChameliumBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2f, 5f).sound(SoundType.STONE));
        registerDefaultState(defaultBlockState().setValue(COLOR, DyeColor.BLACK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        DyeColor dye = readDyeColor(stack);
        return defaultBlockState().setValue(COLOR, dye != null ? dye : DyeColor.BLACK);
    }

    public static DyeColor readDyeColor(ItemStack stack) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed != null) {
            return dyeColorFromRgb(dyed.rgb());
        }
        int legacyColor = ItemColorizer.getColor(stack);
        if (legacyColor >= 0) {
            return dyeColorFromRgb(legacyColor);
        }
        return null;
    }

    public static DyeColor dyeColorFromRgb(int rgb) {
        int bestDist = Integer.MAX_VALUE;
        DyeColor best = null;
        for (DyeColor dye : DyeColor.values()) {
            int dc = dye.getTextColor();
            int dr = ((rgb >> 16) & 0xFF) - ((dc >> 16) & 0xFF);
            int dg = ((rgb >> 8) & 0xFF) - ((dc >> 8) & 0xFF);
            int db = (rgb & 0xFF) - (dc & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = dye;
            }
        }
        return best;
    }

    public static DyeColor dyeColorFromFrequency(int frequency) {
        return DyeColor.byId(frequency & 0xF);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        DyeColor dye = state.getValue(COLOR);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(dye.getTextColor(), false));
        return stack;
    }
}
