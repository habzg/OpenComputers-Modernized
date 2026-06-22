package li.cil.oc.core.impl.util;


import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidTank;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class FluidUtils {
    private static FluidTransferHandler handler;

    private FluidUtils() {
    }

    public static void setHandler(FluidTransferHandler h) {
        handler = h;
    }

    public static FluidHandler fluidHandlerAt(BlockPosition position) {
        return handler != null ? handler.fluidHandlerAt(position) : null;
    }

    public static int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide, int limit, int sourceTank) {
        return handler != null ? handler.transferBetweenFluidHandlersAt(sourcePos, sourceSide, sinkPos, sinkSide, limit, sourceTank) : 0;
    }

    public static FluidTank tankFrom(MultiTank multiTank, int index) {
        return handler != null ? handler.tankFrom(multiTank, index) : null;
    }

    public static FluidHandler fluidHandlerIn(ItemStack stack) {
        return handler != null ? handler.fluidHandlerIn(stack) : null;
    }

    public interface FluidTransferHandler {
        FluidHandler fluidHandlerAt(BlockPosition position);

        FluidHandler fluidHandlerIn(ItemStack stack);

        @SuppressWarnings("unused")
        int transferBetweenFluidHandlers(FluidHandler source, Direction sourceSide, FluidHandler sink, Direction sinkSide, int limit, int sourceTank);

        int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide, int limit, int sourceTank);

        FluidTank tankFrom(MultiTank multiTank, int index);
    }
}
