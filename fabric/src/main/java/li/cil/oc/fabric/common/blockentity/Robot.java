package li.cil.oc.fabric.common.blockentity;

import java.util.HashSet;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.fabric.common.init.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class Robot extends li.cil.oc.core.impl.common.blockentity.Robot implements FluidHandler {
    public Robot(BlockPos pos, BlockState state) {
        super(BlockEntities.ROBOT, pos, state);
    }

    @Override
    public void setBlockPos(BlockPos pos) {
        this.worldPosition = pos;
    }

    @Override
    protected Player createAgentPlayer(ServerLevel sl) {
        return new li.cil.oc.fabric.server.agent.Player(sl, this);
    }

    @Override
    protected boolean hasRedstoneCardInInventory() {
        var driver = new li.cil.oc.fabric.integration.opencomputers.DriverRedstoneCard();
        var allSlots = new HashSet<Integer>();
        allSlots.addAll(containerSlots());
        allSlots.addAll(componentSlots());
        for (int s : allSlots) {
            var stack = getItem(s);
            if (!stack.isEmpty() && driver.worksWith(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void postAnalyzeEvent(Node[] nodes, Player player) {
        li.cil.oc.api.event.RobotAnalyzeEvent.EVENT.invoker().onRobotAnalyze(new li.cil.oc.api.event.RobotAnalyzeEvent(this, player));
    }

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        var t = getFluidTank(tankIndex);
        return t != null ? t.getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        var t = getFluidTank(selectedTank);
        return t != null ? t.fill(resource, simulate) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        var t = getFluidTank(selectedTank);
        if (t != null && t.getFluid().hasSameFluid(resource)) return t.drain(resource.amount(), simulate);
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        var t = getFluidTank(selectedTank);
        return t != null ? t.drain(maxDrain, simulate) : FluidStack.EMPTY;
    }
}
