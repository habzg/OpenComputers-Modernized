package li.cil.oc.neoforge.util;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidTank;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

public class FluidTransferHandlerImpl implements FluidUtils.FluidTransferHandler {
    @Override
    public FluidHandler fluidHandlerAt(BlockPosition position) {
        if (position.level() != null) {
            Level world = position.level();
            if (world.isLoaded(position.toBlockPos())) {
                IFluidHandler capHandler = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, position.toBlockPos(), null);
                if (capHandler != null) {
                    return new li.cil.oc.neoforge.util.FluidHandler(capHandler);
                }
                BlockEntity te = world.getBlockEntity(position.toBlockPos());
                if (te instanceof IFluidHandler handler) {
                    return new li.cil.oc.neoforge.util.FluidHandler(handler);
                }
                return new li.cil.oc.neoforge.util.FluidHandler(new GenericBlockWrapper(position));
            }
        }
        return null;
    }

    @Override
    public FluidHandler fluidHandlerAt(BlockPosition position, Direction side) {
        if (position.level() != null) {
            Level world = position.level();
            if (world.isLoaded(position.toBlockPos())) {
                IFluidHandler capHandler = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, position.toBlockPos(), side);
                if (capHandler != null) {
                    return new li.cil.oc.neoforge.util.FluidHandler(capHandler);
                }
                return fluidHandlerAt(position);
            }
        }
        return null;
    }

    @Override
    public FluidHandler fluidHandlerIn(ItemStack stack) {
        if (!stack.isEmpty()) {
            ItemStack oneSized = stack.copy();
            oneSized.setCount(1);
            IFluidHandlerItem handler = oneSized.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (handler != null) {
                return new li.cil.oc.neoforge.util.FluidHandler(handler);
            }
        }
        return null;
    }

    @Override
    public ItemStack fillItem(ItemStack stack, li.cil.oc.core.util.FluidStack resource) {
        if (stack.isEmpty()) return null;
        ItemStack oneSized = stack.copy();
        oneSized.setCount(1);
        IFluidHandlerItem handler = oneSized.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if (handler == null) return null;
        int filled = handler.fill(li.cil.oc.neoforge.util.FluidHandler.toNeo(resource), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) return null;
        return handler.getContainer();
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
    }

    @Override
    public int transferBetweenFluidHandlers(FluidHandler source, Direction sourceSide, FluidHandler sink, Direction sinkSide, int limit, int sourceTank) {
        IFluidHandler neoSource = unwrap(source);
        IFluidHandler neoSink = unwrap(sink);
        if (neoSource == null || neoSink == null) return 0;
        return FluidUtilsOriginal.doTransfer(neoSource, sourceSide, neoSink, sinkSide, limit, sourceTank);
    }

    @Override
    public int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide, int limit, int sourceTank) {
        IFluidHandler sourceHandler = internalHandlerAt(sourcePos);
        if (sourceHandler == null) return 0;
        IFluidHandler sinkHandler = internalHandlerAt(sinkPos);
        if (sinkHandler == null) return 0;
        return FluidUtilsOriginal.doTransfer(sourceHandler, sourceSide, sinkHandler, sinkSide, limit, sourceTank);
    }

    @Override
    public FluidTank tankFrom(MultiTank multiTank, int index) {
        Object tank = multiTank.getFluidTank(index);
        if (tank instanceof FluidTank ft) return ft;
        if (tank instanceof IFluidTank ift) return new li.cil.oc.neoforge.util.FluidTank(ift);
        return null;
    }

    private IFluidHandler unwrap(FluidHandler handler) {
        if (handler instanceof li.cil.oc.neoforge.util.FluidHandler(IFluidHandler delegate)) return delegate;
        return null;
    }

    private IFluidHandler internalHandlerAt(BlockPosition position) {
        if (position.level() != null) {
            Level world = position.level();
            if (world.isLoaded(position.toBlockPos())) {
                IFluidHandler capHandler = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, position.toBlockPos(), null);
                if (capHandler != null) {
                    return capHandler;
                }
                BlockEntity te = world.getBlockEntity(position.toBlockPos());
                if (te instanceof IFluidHandler handler) {
                    return handler;
                }
                return new GenericBlockWrapper(position);
            }
        }
        return null;
    }

    private record GenericBlockWrapper(BlockPosition position) implements IFluidHandler {

        IFluidHandler currentWrapper() {
            if (position.level() == null) return null;
            Level world = position.level();
            if (!world.isLoaded(position.toBlockPos())) return null;
            Block block = world.getBlockState(position.toBlockPos()).getBlock();
            if (block instanceof LiquidBlock) {
                return new FluidBlockWrapper(position, (LiquidBlock) block);
            }
            return null;
        }

        @Override
        public int getTanks() {
            IFluidHandler w = currentWrapper();
            return w != null ? w.getTanks() : 0;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            IFluidHandler w = currentWrapper();
            return w != null ? w.getFluidInTank(tank) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler w = currentWrapper();
            return w != null ? w.getTankCapacity(tank) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            IFluidHandler w = currentWrapper();
            return w != null && w.isFluidValid(tank, stack);
        }

        @Override
        public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
            IFluidHandler w = currentWrapper();
            return w != null ? w.fill(resource, action) : 0;
        }

        @Override
        public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
            IFluidHandler w = currentWrapper();
            return w != null ? w.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            IFluidHandler w = currentWrapper();
            return w != null ? w.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    private record FluidBlockWrapper(BlockPosition position, LiquidBlock block) implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (position.level() != null) {
                FluidState state = position.level().getFluidState(position.toBlockPos());
                return new FluidStack(state.getType(), 1000);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, @NotNull FluidAction action) {
            return drain(resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            if (position.level() != null) {
                FluidState state = position.level().getFluidState(position.toBlockPos());
                return new FluidStack(state.getType(), Math.min(maxDrain, 1000));
            }
            return FluidStack.EMPTY;
        }
    }
}
