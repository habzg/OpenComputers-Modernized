package li.cil.oc.core.impl.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class Keyboard extends SimpleBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public Keyboard() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2f, 5f).sound(SoundType.STONE));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (placer != null) setRotationFromEntityPitchAndYaw(world, pos, placer);
        setFacing(world, pos, state.getValue(FACING));
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        var result = adjacencyInfo(world, pos);
        if (result != null) {
            var screen = (Screen) world.getBlockState(result.blockPos()).getBlock();
            return screen.rightClick(world, result.blockPos(), player, result.facing(), hitX, hitY, hitZ, true);
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    private AdjacencyResult adjacencyInfo(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (!(te instanceof li.cil.oc.core.impl.common.blockentity.Keyboard keyboard)) return null;
        Direction facing = world.getBlockState(pos).getValue(FACING);
        BlockPos behindPos = pos.relative(facing.getOpposite());
        if (world.getBlockState(behindPos).getBlock() instanceof Screen) {
            return new AdjacencyResult(behindPos, facing.getOpposite());
        }
        Direction forward = switch (facing) {
            case UP, DOWN -> keyboard.yaw();
            default -> Direction.UP;
        };
        BlockPos forwardPos = pos.relative(forward);
        if (world.getBlockState(forwardPos).getBlock() instanceof Screen) {
            return new AdjacencyResult(forwardPos, forward);
        }
        if (facing != Direction.UP && facing != Direction.DOWN) {
            BlockPos belowPos = pos.relative(forward.getOpposite());
            if (world.getBlockState(belowPos).getBlock() instanceof Screen) {
                return new AdjacencyResult(belowPos, forward.getOpposite());
            }
        }
        return null;
    }

    private record AdjacencyResult(BlockPos blockPos, Direction facing) {
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        if (!world.getBlockState(supportPos).isFaceSturdy(world, supportPos, facing)) return false;
        BlockEntity te = world.getBlockEntity(supportPos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) {
            return screen.facing() != facing;
        }
        return true;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (!world.isClientSide && !state.canSurvive(world, pos)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Keyboard(pos, state);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH -> box(1, 4, 15, 15, 12, 16);
            case SOUTH -> box(1, 4, 0, 15, 12, 1);
            case WEST -> box(15, 4, 1, 16, 12, 15);
            case EAST -> box(0, 4, 1, 1, 12, 15);
            case UP -> box(1, 0, 4, 15, 1, 12);
            case DOWN -> box(1, 15, 4, 15, 16, 12);
        };
    }
}
