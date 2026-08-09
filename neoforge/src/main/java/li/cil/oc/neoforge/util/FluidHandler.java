package li.cil.oc.neoforge.util;

import li.cil.oc.core.util.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public record FluidHandler(IFluidHandler delegate) implements li.cil.oc.core.util.FluidHandler {

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        net.neoforged.neoforge.fluids.FluidStack neo = delegate.getFluidInTank(tank);
        return neo.isEmpty() ? FluidStack.EMPTY : fromNeo(neo);
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        net.neoforged.neoforge.fluids.FluidStack neo = resource.isEmpty() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : toNeo(resource);
        return delegate.fill(neo, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        net.neoforged.neoforge.fluids.FluidStack neo = resource.isEmpty() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : toNeo(resource);
        net.neoforged.neoforge.fluids.FluidStack result = delegate.drain(neo, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        return result.isEmpty() ? FluidStack.EMPTY : fromNeo(result);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        net.neoforged.neoforge.fluids.FluidStack result = delegate.drain(maxDrain, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        return result.isEmpty() ? FluidStack.EMPTY : fromNeo(result);
    }

    public static net.neoforged.neoforge.fluids.FluidStack toNeo(FluidStack stack) {
        return new net.neoforged.neoforge.fluids.FluidStack(
                BuiltInRegistries.FLUID.get(ResourceLocation.parse(stack.fluidName())),
                stack.amount()
        );
    }

    public static FluidStack fromNeo(net.neoforged.neoforge.fluids.FluidStack stack) {
        return new FluidStack(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(), stack.getAmount(), !stack.getComponents().isEmpty());
    }
}
