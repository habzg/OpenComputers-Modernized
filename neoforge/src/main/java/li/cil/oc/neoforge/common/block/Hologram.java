package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Hologram extends SimpleBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public final int tier;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);

    public Hologram(int tier) {
        super();
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        if (Settings.get().hologramLight) {
            var te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.tileentity.Hologram hologram && hologram.hasPower) {
                return 8;
            }
        }
        return super.getLightEmission(state, world, pos);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Hologram(pos, state, this.tier);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        var tileType = li.cil.oc.neoforge.common.init.TileEntities.HOLOGRAM.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.tileentity.Hologram) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in hologram tick", e);
            }
        } : null;
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            var te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.tileentity.Hologram hologram) {
                hologram.syncFromBlockState(state);
            }
        }
    }

    @Override
    public void onBlockStateChange(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState oldState, @NotNull BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        var te = level.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Hologram hologram) {
            hologram.syncFromBlockState(newState);
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
