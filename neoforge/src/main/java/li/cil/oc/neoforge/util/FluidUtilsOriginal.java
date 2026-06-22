package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public final class FluidUtilsOriginal {
    private FluidUtilsOriginal() {
    }

    public static int doTransfer(IFluidHandler source, Direction sourceSide, IFluidHandler sink, Direction sinkSide, int limit) {
        return doTransfer(source, sourceSide, sink, sinkSide, limit, -1);
    }

    @SuppressWarnings("unused")
    public static int doTransfer(IFluidHandler source, Direction sourceSide, IFluidHandler sink, Direction sinkSide, int limit, int sourceTank) {
        int tanks = source.getTanks();
        FluidStack srcFluid = (sourceTank < 0 || tanks <= sourceTank) ? null : source.getFluidInTank(sourceTank);

        boolean nullFluid = srcFluid == null || srcFluid.isEmpty();
        FluidStack drained;
        if (nullFluid) {
            drained = source.drain(limit, IFluidHandler.FluidAction.SIMULATE);
        } else {
            srcFluid.setAmount(limit);
            drained = source.drain(srcFluid, IFluidHandler.FluidAction.SIMULATE);
        }
        if (!drained.isEmpty()) {
            int filled = sink.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (nullFluid) {
                sink.fill(source.drain(filled, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            } else {
                srcFluid.setAmount(filled);
                sink.fill(source.drain(srcFluid, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            }
            return filled;
        }
        return 0;
    }

    public static IFluidHandler fluidHandlerAt(BlockPosition position) {
        if (position.level() != null) {
            Level world = position.level();
            if (world.isLoaded(position.toBlockPos())) {
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
