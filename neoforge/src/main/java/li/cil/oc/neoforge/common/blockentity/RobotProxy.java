package li.cil.oc.neoforge.common.blockentity;

import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.neoforge.common.init.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class RobotProxy extends li.cil.oc.core.impl.common.blockentity.RobotProxy implements IFluidHandler {
    public RobotProxy(BlockPos pos, BlockState state) {
        this(new Robot(pos, state), pos, state);
    }

    public RobotProxy(Robot robot, BlockPos pos, BlockState state) {
        super(BlockEntities.ROBOT.get(), robot, pos, state);
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        if (robot.getLevel() == null) {
            var level = getLevel();
            if (level != null) robot.setLevel(level);
            robot.setBlockPos(getBlockPos());
            robot.proxy = this;
        }
        super.handleUpdateTag(tag, provider);
    }

    @Override
    public void initialize() {
        if (isServer()) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        setupRobotProxy();
    }

    private Robot neoRobot() {
        return (Robot) robot;
    }

    @Override
    public int getTanks() {
        return robot.getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return neoRobot().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return robot.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return neoRobot().isFluidValid(tank, stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return neoRobot().fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return neoRobot().drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        return neoRobot().drain(maxDrain, action);
    }
}
