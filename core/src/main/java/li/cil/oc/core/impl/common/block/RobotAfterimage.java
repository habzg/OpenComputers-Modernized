package li.cil.oc.core.impl.common.block;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.RobotBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public abstract class RobotAfterimage extends SimpleBlock {
    protected RobotAfterimage(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public abstract RobotBase findMovingRobot(BlockGetter world, BlockPos pos);

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        var robot = findMovingRobot(world, pos);
        if (robot != null) {
            var fullDx = robot.getBlockPos().getX() - robot.moveFromX;
            var fullDy = robot.getBlockPos().getY() - robot.moveFromY;
            var fullDz = robot.getBlockPos().getZ() - robot.moveFromZ;
            var remaining = robot.animationTicksTotal > 0 ? (double) robot.animationTicksLeft / (double) robot.animationTicksTotal : 0.0;
            return RobotProxy.SHAPE.move(fullDx * (1.0 - remaining), fullDy * (1.0 - remaining), fullDz * (1.0 - remaining));
        }
        return Shapes.block();
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        return 1.0f;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, world, pos, oldState, movedByPiston);
        world.scheduleTick(pos, this, Math.max((int) (OCSettings.get().moveDelay * 20), 1) - 1);
    }

    @Override
    public void tick(@NotNull BlockState state, ServerLevel world, @NotNull BlockPos pos, @NotNull RandomSource random) {
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        var robot = findMovingRobot(world, pos);
        if (robot != null && robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
            var robotBlock = li.cil.oc.api.Items.get(li.cil.oc.core.Constants.BlockName.Robot).block();
            if (robotBlock instanceof AbstractBlock base) {
                return base.onBlockActivated(world, robot.getBlockPos(), player, side, hitX, hitY, hitZ, hand);
            }
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader world, @NotNull BlockPos pos, @NotNull BlockState state) {
        var robot = findMovingRobot(world, pos);
        if (robot != null) {
            return robot.info.createItemStack();
        }
        return ItemStack.EMPTY;
    }
}
