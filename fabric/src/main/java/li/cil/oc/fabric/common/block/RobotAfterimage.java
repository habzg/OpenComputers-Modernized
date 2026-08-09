package li.cil.oc.fabric.common.block;

import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.util.Rarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class RobotAfterimage extends li.cil.oc.core.impl.common.block.RobotAfterimage {
    public RobotAfterimage() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 10f).noOcclusion().noCollission().replaceable().air());
    }

    @Override
    public li.cil.oc.core.impl.common.blockentity.RobotBase findMovingRobot(@NotNull BlockGetter world, @NotNull BlockPos pos) {
        for (var side : Direction.values()) {
            var tpos = pos.relative(side);
            if (world instanceof Level l && !l.isLoaded(tpos)) continue;
            var te = world.getBlockEntity(tpos);
            if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy) {
                var robot = proxy.robot;
                if (robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
                    return robot;
                }
            }
        }
        return null;
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        var data = new RobotData(stack);
        return Rarity.byTier(data.tier);
    }
}
