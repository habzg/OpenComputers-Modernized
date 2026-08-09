package li.cil.oc.core.impl.common.block;

import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.impl.common.block.traits.CustomDrops;
import li.cil.oc.core.impl.util.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class Cable extends SimpleBlock implements CustomDrops<li.cil.oc.core.impl.common.blockentity.Cable> {
    protected static final VoxelShape CENTER = box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape ARM_DOWN = box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape ARM_UP = box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape ARM_NORTH = box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape ARM_SOUTH = box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape ARM_WEST = box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape ARM_EAST = box(10, 6, 6, 16, 10, 10);

    public Cable() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 5f).noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Cable(pos, state);
    }

    @Override
    public Class<li.cil.oc.core.impl.common.blockentity.Cable> getBlockClass() {
        return li.cil.oc.core.impl.common.blockentity.Cable.class;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape shape = CENTER;
        int selfColor = Color.LightGray;
        BlockEntity te = getter.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Cable cable) {
            selfColor = cable.color();
        }
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos.relative(side);
            BlockEntity neighbor = getter.getBlockEntity(neighborPos);
            boolean isOCNeighbor = false;
            int neighborColor = Color.LightGray;
            if (neighbor instanceof li.cil.oc.api.network.Environment) {
                if (!(neighbor instanceof li.cil.oc.core.impl.common.blockentity.RobotProxy)
                        && (!(neighbor instanceof SidedEnvironment sideEnv) || sideEnv.canConnect(side))) {
                    isOCNeighbor = true;
                    if (neighbor instanceof li.cil.oc.core.impl.common.blockentity.Cable neighborCable) {
                        neighborColor = neighborCable.color();
                    }
                }
            }
            if (isOCNeighbor) {
                if (selfColor == neighborColor || selfColor == Color.LightGray || neighborColor == Color.LightGray) {
                    shape = Shapes.joinUnoptimized(shape, armFor(side), BooleanOp.OR);
                }
            }
        }
        return shape;
    }

    public static VoxelShape armFor(Direction side) {
        return switch (side) {
            case NORTH -> ARM_NORTH;
            case SOUTH -> ARM_SOUTH;
            case WEST -> ARM_WEST;
            case EAST -> ARM_EAST;
            case DOWN -> ARM_DOWN;
            case UP -> ARM_UP;
        };
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        world.sendBlockUpdated(pos, state, state, 3);
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Cable cable) {
            return cable.createItemStack();
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public void doCustomInit(li.cil.oc.core.impl.common.blockentity.Cable blockEntity, LivingEntity player, ItemStack stack) {
        var level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            blockEntity.fromItemStack(stack);
        }
    }

    @Override
    public void doCustomDrops(li.cil.oc.core.impl.common.blockentity.Cable blockEntity, Player player, boolean willHarvest) {
        if (!player.getAbilities().instabuild) {
            var level = blockEntity.getLevel();
            if (level != null) {
                Block.popResource(level, blockEntity.getBlockPos(), blockEntity.createItemStack());
            }
        }
    }
}
