package li.cil.oc.core.util;

public interface FluidTank {
    FluidStack getFluid();

    int getCapacity();

    int getFluidAmount();

    int getSpace();

    int fill(FluidStack resource, boolean simulate);

    FluidStack drain(int maxDrain, boolean simulate);
}
