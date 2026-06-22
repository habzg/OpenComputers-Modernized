package li.cil.oc.core.util;

public record FluidStack(String fluidName, int amount) {
    public static final FluidStack EMPTY = new FluidStack("", 0);

    public FluidStack(String fluidName, int amount) {
        this.fluidName = fluidName != null ? fluidName : "";
        this.amount = amount;
    }

    public boolean isEmpty() {
        return fluidName.isEmpty() || amount <= 0;
    }

    public boolean hasSameFluid(FluidStack other) {
        return other != null && fluidName.equals(other.fluidName);
    }

    public FluidStack copyWithAmount(int newAmount) {
        return new FluidStack(fluidName, newAmount);
    }
}
