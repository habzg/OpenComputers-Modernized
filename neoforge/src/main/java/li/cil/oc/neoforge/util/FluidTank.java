package li.cil.oc.neoforge.util;

import li.cil.oc.core.util.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public record FluidTank(IFluidTank delegate) implements li.cil.oc.core.util.FluidTank {


    public FluidStack getFluid() {
        net.neoforged.neoforge.fluids.FluidStack neo = delegate.getFluid();
        return neo.isEmpty() ? FluidStack.EMPTY : FluidHandler.fromNeo(neo);
    }

    public int getCapacity() {
        return delegate.getCapacity();
    }

    public int getFluidAmount() {
        return delegate.getFluidAmount();
    }

    public int getSpace() {
        return delegate.getCapacity() - delegate.getFluidAmount();
    }

    public int fill(FluidStack resource, boolean simulate) {
        net.neoforged.neoforge.fluids.FluidStack neo = resource.isEmpty() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : FluidHandler.toNeo(resource);
        return delegate.fill(neo, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    public FluidStack drain(int maxDrain, boolean simulate) {
        net.neoforged.neoforge.fluids.FluidStack neo = delegate.drain(maxDrain, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        return neo.isEmpty() ? FluidStack.EMPTY : FluidHandler.fromNeo(neo);
    }
}
