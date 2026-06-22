package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;


public class Waypoint extends RedstoneAware {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public Waypoint() {
        super();
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        setFacing(world, pos, state.getValue(FACING));
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            if (world.isClientSide) {
                li.cil.oc.neoforge.client.GuiHandler.openScreen(GuiType.Waypoint, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }


    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Waypoint(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        var tileType = li.cil.oc.neoforge.common.init.TileEntities.WAYPOINT.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.tileentity.Waypoint) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in waypoint tick", e);
            }
        } : null;
    }
}
