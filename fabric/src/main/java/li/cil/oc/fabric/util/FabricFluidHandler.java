package li.cil.oc.fabric.util;

import li.cil.oc.core.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public record FabricFluidHandler(Storage<FluidVariant> delegate) implements li.cil.oc.core.util.FluidHandler {
    private static final long MB_TO_DROPLETS = FluidConstants.BUCKET / 1000;

    @Override
    public int getTanks() {
        int count = 0;
        for (var ignored : delegate) { count++; }
        return count;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int i = 0;
        for (var view : delegate) {
            if (i == tank) {
                FluidVariant fluid = view.getResource();
                return fluid.isBlank() ? FluidStack.EMPTY : fromFabric(fluid, view.getAmount());
            }
            i++;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        int i = 0;
        for (var view : delegate) {
            if (i == tank) return (int) (view.getCapacity() / MB_TO_DROPLETS);
            i++;
        }
        return 0;
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;
        FluidVariant fluid = toFabric(resource);
        long droplets = resource.amount() * MB_TO_DROPLETS;
        try (Transaction tx = Transaction.openOuter()) {
            long filled = delegate.insert(fluid, droplets, tx);
            if (!simulate) tx.commit();
            return (int) (filled / MB_TO_DROPLETS);
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        FluidVariant fluid = toFabric(resource);
        long droplets = resource.amount() * MB_TO_DROPLETS;
        try (Transaction tx = Transaction.openOuter()) {
            long drained = delegate.extract(fluid, droplets, tx);
            if (!simulate) tx.commit();
            return drained > 0 ? new FluidStack(resource.fluidName(), (int) (drained / MB_TO_DROPLETS)) : FluidStack.EMPTY;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        long droplets = maxDrain * MB_TO_DROPLETS;
        try (Transaction tx = Transaction.openOuter()) {
            for (var view : delegate) {
                FluidVariant fluid = view.getResource();
                if (!fluid.isBlank()) {
                    long drained = delegate.extract(fluid, droplets, tx);
                    if (!simulate) tx.commit();
                    return drained > 0 ? fromFabric(fluid, drained) : FluidStack.EMPTY;
                }
            }
        }
        return FluidStack.EMPTY;
    }

    public static FluidVariant toFabric(FluidStack stack) {
        return FluidVariant.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse(stack.fluidName())));
    }

    public static FluidStack fromFabric(FluidVariant fluid, long droplets) {
        return new FluidStack(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString(), (int) (droplets / MB_TO_DROPLETS), !fluid.getComponents().isEmpty());
    }
}
