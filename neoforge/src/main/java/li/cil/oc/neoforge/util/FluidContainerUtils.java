package li.cil.oc.neoforge.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

public final class FluidContainerUtils {

    public static IFluidHandler fluidHandlerIn(Container inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (!stack.isEmpty()) {
            ItemStack oneSizedStack = stack.copy();
            oneSizedStack.setCount(1);
            IFluidHandlerItem fluidHandler = oneSizedStack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (fluidHandler != null) {
                return new FluidContainerItemWrapper(oneSizedStack, fluidHandler);
            }
        }
        return null;
    }

    public static IFluidHandler replayableFluidHandler(IFluidHandler handler) {
        return replayableFluidHandler(handler, true);
    }

    public static IFluidHandler replayableFluidHandler(IFluidHandler handler, boolean simulate) {
        return new ReplayableFluidHandler(handler, simulate);
    }

    public static ItemStack getContainerResult(IFluidHandler container) {
        if (container instanceof ContainerWrapper) {
            return ((ContainerWrapper) container).getResult();
        }
        return null;
    }

    public static void replay(IFluidHandler replayable, IFluidHandler handler) {
        if (replayable instanceof ReplayableFluidHandler r) {
            r.replay(handler);
        }
    }

    private interface ContainerWrapper extends IFluidHandler {
        ItemStack getResult();
    }

    private static class FluidContainerItemWrapper implements ContainerWrapper {
        final ItemStack stack;
        final IFluidHandlerItem fluidHandler;
        boolean dirty = false;

        FluidContainerItemWrapper(ItemStack stack, IFluidHandlerItem fluidHandler) {
            this.stack = stack;
            this.fluidHandler = fluidHandler;
        }

        @Override
        public int fill(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
            dirty = true;
            return fluidHandler.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
            return fluidHandler.drain(resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, IFluidHandler.@NotNull FluidAction action) {
            return fluidHandler.drain(maxDrain, action);
        }

        @Override
        public int getTanks() {
            return fluidHandler.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return fluidHandler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidHandler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return fluidHandler.isFluidValid(tank, stack);
        }

        @Override
        public ItemStack getResult() {
            return dirty ? stack : null;
        }
    }

    private static class ReplayableFluidHandler implements IFluidHandler {
        final IFluidHandler handler;
        final boolean simulate;
        final List<java.util.function.Consumer<IFluidHandler>> actions = new ArrayList<>();

        ReplayableFluidHandler(IFluidHandler handler, boolean simulate) {
            this.handler = handler;
            this.simulate = simulate;
        }

        void replay(IFluidHandler target) {
            for (java.util.function.Consumer<IFluidHandler> action : actions) {
                action.accept(target);
            }
        }

        @Override
        public int fill(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
            IFluidHandler.FluidAction realAction = (action == IFluidHandler.FluidAction.EXECUTE && !simulate)
                    ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
            actions.add(h -> h.fill(resource, realAction));
            return handler.fill(resource, realAction);
        }

        @Override
        public @NotNull FluidStack drain(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
            IFluidHandler.FluidAction realAction = (action == IFluidHandler.FluidAction.EXECUTE && !simulate)
                    ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
            actions.add(h -> h.drain(resource, realAction));
            return handler.drain(resource, realAction);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, IFluidHandler.@NotNull FluidAction action) {
            IFluidHandler.FluidAction realAction = (action == IFluidHandler.FluidAction.EXECUTE && !simulate)
                    ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
            actions.add(h -> h.drain(maxDrain, realAction));
            return handler.drain(maxDrain, realAction);
        }

        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return handler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return handler.isFluidValid(tank, stack);
        }
    }
}
