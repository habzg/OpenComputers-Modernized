package li.cil.oc.core.impl.common.block;

import java.util.List;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Hologram extends SimpleBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public final int tier;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);

    public static BlockEntityType<?> TYPE;

    public static int getLuminance(BlockState state) {
        OCSettings s = OCSettings.get();
        return s != null && s.hologramLight ? 15 : 0;
    }

    public Hologram(int tier, BlockEntityType<?> blockType) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 5f).sound(net.minecraft.world.level.block.SoundType.METAL).lightLevel(Hologram::getLuminance));
        this.tier = tier;
        TYPE = blockType;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(POWERED, false));
    }

    public Hologram(int tier) {
        this(tier, null);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        boolean powered = state.getValue(POWERED);
        boolean newPowered = level.hasNeighborSignal(pos);
        if (powered != newPowered) {
            level.setBlock(pos, state.setValue(POWERED, newPowered), 2);
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Hologram(pos, state, this.tier);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == TYPE ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.blockentity.Hologram) te).updateEntity();
            } catch (Exception e) {
                Log.get().warn("Error in hologram tick", e);
            }
        } : null;
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            var te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Hologram hologram) {
                hologram.syncFromBlockState(state);
            }
        }
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        return Rarity.byTier(tier);
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get("Hologram" + tier));
    }
}
