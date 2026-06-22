package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.util.Rarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class RobotAfterimage extends SimpleBlock {
    public RobotAfterimage() {
        super();
    }

    public li.cil.oc.neoforge.common.tileentity.RobotProxy findMovingRobot(BlockGetter world, BlockPos pos) {
        for (var side : Direction.values()) {
            var tpos = pos.relative(side);
            if (world instanceof Level l && !l.isLoaded(tpos)) continue;
            var te = world.getBlockEntity(tpos);
            if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
                var robot = proxy.robot;
                if (robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
                    return proxy;
                }
            }
        }
        return null;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        var proxy = findMovingRobot(world, pos);
        if (proxy != null) {
            var robot = proxy.robot;
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
    public boolean isAir(@NotNull BlockState state) {
        return true;
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        return 1.0f;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, world, pos, oldState, movedByPiston);
        world.scheduleTick(pos, this, Math.max((int) (Settings.get().moveDelay * 20), 1) - 1);
    }

    @Override
    public void tick(@NotNull BlockState state, ServerLevel world, @NotNull BlockPos pos, net.minecraft.util.@NotNull RandomSource random) {
        world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, net.minecraft.core.Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        var proxy = findMovingRobot(world, pos);
        if (proxy != null) {
            var robot = proxy.robot;
            if (robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
                var robotBlock = li.cil.oc.api.Items.get(li.cil.oc.core.Constants.BlockName.Robot).block();
                if (robotBlock instanceof SimpleBlock sb) {
                    return sb.onBlockActivated(world, robot.getBlockPos(), player, side, hitX, hitY, hitZ, hand);
                }
            }
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, net.minecraft.world.level.material.@NotNull FluidState fluid) {
        var proxy = findMovingRobot(world, pos);
        if (proxy != null) {
            var robot = proxy.robot;
            if (robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader world, @NotNull BlockPos pos, @NotNull Player player) {
        var proxy = findMovingRobot(world, pos);
        if (proxy != null) {
            return proxy.robot.info.createItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        var data = new RobotData(stack);
        return Rarity.byTier(data.tier);
    }

}
