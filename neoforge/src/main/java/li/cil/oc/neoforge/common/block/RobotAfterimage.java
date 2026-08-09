package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.util.Rarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

public class RobotAfterimage extends li.cil.oc.core.impl.common.block.RobotAfterimage {
    public RobotAfterimage() {
        super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(2f, 10f).noOcclusion());
    }

    @Override
    public li.cil.oc.core.impl.common.blockentity.RobotBase findMovingRobot(BlockGetter world, BlockPos pos) {
        for (var side : Direction.values()) {
            var tpos = pos.relative(side);
            if (world instanceof Level l && !l.isLoaded(tpos)) continue;
            var te = world.getBlockEntity(tpos);
            if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy) {
                var robot = proxy.robot;
                if (robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
                    return robot;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isAir(@NotNull BlockState ignoredState) {
        return true;
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, @NotNull FluidState fluid) {
        var robot = findMovingRobot(world, pos);
        if (robot != null && robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return false;
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        var data = new RobotData(stack);
        return Rarity.byTier(data.tier);
    }
}
