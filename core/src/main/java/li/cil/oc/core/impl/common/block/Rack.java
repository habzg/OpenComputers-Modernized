package li.cil.oc.core.impl.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class Rack extends RedstoneAware implements PowerAcceptor, GUI, StateAware {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape[] SHAPES_BY_FACING = new VoxelShape[6];

    static {
        var slabs = new VoxelShape[]{
                box(0, 0, 0, 16, 1, 16),
                box(0, 15, 0, 16, 16, 16),
                box(0, 0, 0, 16, 16, 1),
                box(0, 0, 15, 16, 16, 16),
                box(0, 0, 0, 1, 16, 16),
                box(15, 0, 0, 16, 16, 16)
        };
        var inner = box(0.5, 0.5, 0.5, 15.5, 15.5, 15.5);
        for (int i = 0; i < 6; i++) {
            var shape = inner;
            for (int j = 0; j < 6; j++) {
                if (i != j) {
                    shape = Shapes.or(shape, slabs[j]);
                }
            }
            SHAPES_BY_FACING[i] = shape;
        }
    }

    public static BlockEntityType<?> TYPE;

    public Rack(BlockEntityType<?> blockType) {
        super();
        TYPE = blockType;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public Rack() {
        super();
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, net.minecraft.world.entity.LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        setFacing(world, pos, state.getValue(FACING));
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPES_BY_FACING[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public int guiType() {
        return GuiType.Rack;
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().serverRackRate;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Rack(pos, state);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Rack rack) {
            rack.onNeighborChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return type == TYPE ? (lvl, pos, st, te) -> ((li.cil.oc.core.impl.common.blockentity.Rack) te).updateEntity() : null;
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Rack rack) {
            var slotOpt = rack.slotAt(side, hitX, hitY, hitZ);
            if (slotOpt.isPresent()) {
                int slot = slotOpt.get();
                var hitVec = new Vec3(
                        (int) (hitX * 16f) / 16f,
                        (int) (hitY * 16f) / 16f,
                        (int) (hitZ * 16f) / 16f
                );
                float rotation = switch (side) {
                    case WEST -> (float) Math.toRadians(90);
                    case NORTH -> (float) Math.toRadians(180);
                    case EAST -> (float) Math.toRadians(270);
                    default -> 0;
                };
                var localHitVec = rotate(hitVec.add(-0.5 + 1 / 32f, -0.5 + 1 / 32f, -0.5 + 1 / 32f), rotation)
                        .add(0.5 - 1 / 32f, 0.5 - 1 / 32f, 0.5 - 1 / 32f);
                int globalX = (int) (localHitVec.x * 16.05f);
                int globalY = (int) (localHitVec.y * 16.05f);
                int localX = (side.getAxis() != Axis.Z ? 15 - globalX : globalX) - 1;
                int localY = (15 - globalY) - 2 - 3 * slot;
                if (localX >= 0 && localX < 14 && localY >= 0 && localY < 3) {
                    var mountable = rack.getMountable(slot);
                    if (mountable != null && mountable.onActivate(player, hand, player.getItemInHand(hand), localX / 14f, localY / 3f)) {
                        return true;
                    }
                }
            }
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    private static Vec3 rotate(Vec3 v, float t) {
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }
}
