package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.UpgradeTankBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;


public class UpgradeTank extends UpgradeTankBase implements IFluidHandler, IFluidTank {
    private FluidStack fluid = FluidStack.EMPTY;

    public UpgradeTank(EnvironmentHost owner, int capacity) {
        super(owner, capacity);
    }

    @Override
    protected void loadTankNbt(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        fluid = FluidStack.parse(provider, nbt).orElse(FluidStack.EMPTY);
    }

    @Override
    protected void saveTankNbt(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        if (!fluid.isEmpty()) {
            fluid.save(provider, nbt);
        }
    }

    public @NotNull FluidStack getFluid() {
        return fluid;
    }

    public int getFluidAmount() {
        return fluid.getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return fluid;
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public boolean isFluidValid(@NotNull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        int amount = Math.min(resource.getAmount(), capacity - fluid.getAmount());
        if (amount > 0 && action.execute()) {
            if (fluid.isEmpty()) {
                fluid = new FluidStack(resource.getFluid(), amount);
            } else {
                fluid.grow(amount);
            }
            node.sendToVisible("computer.signal", "tank_changed", tankIndex(), amount);
        }
        return amount;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
        if (resource.isEmpty() || !resource.is(fluid.getFluid())) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, IFluidHandler.@NotNull FluidAction action) {
        int drained = Math.min(maxDrain, fluid.getAmount());
        FluidStack result = new FluidStack(fluid.getFluid(), drained);
        if (drained > 0 && action.execute()) {
            fluid.shrink(drained);
            if (fluid.isEmpty()) {
                fluid = FluidStack.EMPTY;
            }
            node.sendToVisible("computer.signal", "tank_changed", tankIndex(), -drained);
        }
        return result;
    }
}
