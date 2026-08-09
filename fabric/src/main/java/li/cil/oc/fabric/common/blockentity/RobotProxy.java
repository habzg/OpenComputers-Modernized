package li.cil.oc.fabric.common.blockentity;

import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.fabric.common.init.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RobotProxy extends li.cil.oc.core.impl.common.blockentity.RobotProxy implements FluidHandler {
    public RobotProxy(BlockPos pos, BlockState state) {
        this(new Robot(pos, state), pos, state);
    }

    public RobotProxy(Robot robot, BlockPos pos, BlockState state) {
        super(BlockEntities.ROBOT, robot, pos, state);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        if (level.isClientSide && robot.getLevel() == null) {
            robot.setLevel(level);
            robot.setBlockPos(getBlockPos());
            robot.proxy = this;
        }
    }

    private FluidHandler fluidRobot() {
        return (FluidHandler) robot;
    }

    @Override
    public int getTanks() {
        return fluidRobot().getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluidRobot().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return fluidRobot().getTankCapacity(tank);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        return fluidRobot().fill(resource, simulate);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        return fluidRobot().drain(resource, simulate);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        return fluidRobot().drain(maxDrain, simulate);
    }
}
