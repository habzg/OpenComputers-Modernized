package li.cil.oc.core.util;


public interface FluidHandler {
    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    int fill(FluidStack resource, boolean simulate);

    FluidStack drain(FluidStack resource, boolean simulate);

    FluidStack drain(int maxDrain, boolean simulate);
}
