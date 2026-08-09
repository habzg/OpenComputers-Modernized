package li.cil.oc.fabric.common.block;

import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.fabric.common.blockentity.RobotProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class Cable extends li.cil.oc.core.impl.common.block.Cable {
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState ignoredState, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull CollisionContext ignoredContext) {
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
            if (neighbor instanceof li.cil.oc.api.network.Environment && !(neighbor instanceof RobotProxy)) {
                if (!(neighbor instanceof SidedEnvironment sideEnv) || sideEnv.canConnect(side)) {
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
}
