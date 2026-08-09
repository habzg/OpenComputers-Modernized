package li.cil.oc.neoforge.common.blockentity;

import java.util.HashSet;
import li.cil.oc.neoforge.common.init.BlockEntities;
import li.cil.oc.neoforge.util.FluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class Robot extends li.cil.oc.core.impl.common.blockentity.Robot implements IFluidHandler {
    public Robot(BlockPos pos, BlockState state) {
        super(BlockEntities.ROBOT.get(), pos, state);
    }

    @Override
    public void setBlockPos(BlockPos pos) {
        this.worldPosition = pos;
    }

    @Override
    protected Player createAgentPlayer(ServerLevel sl) {
        return new li.cil.oc.neoforge.server.agent.Player(sl, this);
    }

    @Override
    protected boolean hasRedstoneCardInInventory() {
        var driver = new li.cil.oc.neoforge.integration.opencomputers.DriverRedstoneCard();
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
    protected void postAnalyzeEvent(li.cil.oc.api.network.Node[] ignoredNodes, Player player) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new li.cil.oc.api.event.RobotAnalyzeEvent(this, player));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        onBlockEntityLoad();
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.handleUpdateTag(tag, provider);
        if (proxy != null && proxy.robot.getLevel() == null) {
            var level = getLevel();
            if (level != null) setLevel(level);
            setBlockPos(getBlockPos());
        }
    }

    @Override
    public int getTanks() {
        return tankCount();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tankIndex) {
        var t = getFluidTank(tankIndex);
        if (t == null) return FluidStack.EMPTY;
        var coreFluid = t.getFluid();
        if (coreFluid == null || coreFluid.isEmpty()) return FluidStack.EMPTY;
        return FluidHandler.toNeo(coreFluid);
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        var t = getFluidTank(tankIndex);
        return t != null ? t.getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tankIndex, @NotNull FluidStack stack) {
        return getFluidTank(tankIndex) != null;
    }

    @Override
    public int fill(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
        var t = getFluidTank(selectedTank);
        if (t == null) return 0;
        var coreStack = FluidHandler.fromNeo(resource);
        return t.fill(coreStack, action.simulate());
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
        var t = getFluidTank(selectedTank);
        if (t == null) return FluidStack.EMPTY;
        if (t.getFluid().isEmpty() || !t.getFluid().hasSameFluid(FluidHandler.fromNeo(resource))) return FluidStack.EMPTY;
        var result = t.drain(resource.getAmount(), action.simulate());
        return FluidHandler.toNeo(result);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, IFluidHandler.@NotNull FluidAction action) {
        var t = getFluidTank(selectedTank);
        if (t == null) return FluidStack.EMPTY;
        var result = t.drain(maxDrain, action.simulate());
        return FluidHandler.toNeo(result);
    }
}
