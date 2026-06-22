package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.impl.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;


import static li.cil.oc.core.util.ResultWrapper.result;

public interface InventoryTransfer extends WorldAware, SideRestricted {
    String onTransferContents() ;

    int fluidTransferRate();

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number[, sourceSlot:number[, sinkSlot:number]]]):number -- Transfer some items between two inventories.")
    default Object[] transferItem(Context context, Arguments args) {
        Direction sourceSide = checkSideForAction(args, 0);
        BlockPosition sourcePos = position().offset(sourceSide);
        Direction sinkSide = checkSideForAction(args, 1);
        BlockPosition sinkPos = position().offset(sinkSide);
        int count = ExtendedArguments.optItemCount(args, 2, 64);
        String reason = onTransferContents();
        if (reason != null) return result(null, reason);
        InventoryUtils.TransferExtractor extractor;
        if (args.count() > 3) {
            Container sourceInv = InventoryUtils.inventoryAt(sourcePos);
            if (sourceInv == null) throw new IllegalArgumentException("no inventory");
            Container sinkInv = InventoryUtils.inventoryAt(sinkPos);
            if (sinkInv == null) throw new IllegalArgumentException("no inventory");
            int sourceSlot = ExtendedArguments.checkSlot(args, sourceInv, 3);
            int sinkSlot = ExtendedArguments.optSlot(args, sinkInv, 4, -1);
            extractor = InventoryUtils.getTransferBetweenInventoriesSlotsAt(sourcePos, sourceSide.getOpposite(), sourceSlot, sinkPos, sinkSide.getOpposite(), sinkSlot < 0 ? null : sinkSlot, count);
        } else {
            extractor = InventoryUtils.getTransferBetweenInventoriesAt(sourcePos, sourceSide.getOpposite(), sinkPos, sinkSide.getOpposite(), count);
        }
        if (extractor != null) return result(extractor.extract());
        return result(null, "no inventory");
    }

    @Callback(doc = "function(sourceSide:number, sinkSide:number, sourceSlot:number, sinkSlot:number[, safe:boolean]):boolean -- Swap two inventory slots if and only if both directions succeed.")
    default Object[] swap(Context context, Arguments args) {
        Direction sourceSide = checkSideForAction(args, 0);
        BlockPosition sourcePos = position().offset(sourceSide);
        Direction sinkSide = checkSideForAction(args, 1);
        BlockPosition sinkPos = position().offset(sinkSide);
        String reason = onTransferContents();
        if (reason != null) return result(null, reason);
        Container source = InventoryUtils.inventoryAt(sourcePos);
        if (source == null) return result(null, "no inventory");
        Container sink = InventoryUtils.inventoryAt(sinkPos);
        if (sink == null) return result(null, "no inventory");
        int sourceSlot = ExtendedArguments.checkSlot(args, source, 2);
        int sinkSlot = ExtendedArguments.checkSlot(args, sink, 3);
        boolean safe = args.optBoolean(4, false);
        return result(InventoryUtils.swapBetweenInventoriesSlots(source, sourceSide.getOpposite(), sourceSlot, sink, sinkSide.getOpposite(), sinkSlot, safe));
    }

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number[, sourceTank:number]]):boolean, number -- Transfer some fluid between two tanks.")
    default Object[] transferFluid(Context context, Arguments args) {
        Direction sourceSide = checkSideForAction(args, 0);
        BlockPosition sourcePos = position().offset(sourceSide);
        Direction sinkSide = checkSideForAction(args, 1);
        BlockPosition sinkPos = position().offset(sinkSide);
        int count = ExtendedArguments.optFluidCount(args, 2, Integer.MAX_VALUE);
        int sourceTank = args.optInteger(3, -1);
        String reason = onTransferContents();
        if (reason != null) return result(null, reason);
        int rate = fluidTransferRate();
        if (rate == 0) return result(null, "device has fluid transfer rate of 0");
        int moved = FluidUtils.transferBetweenFluidHandlersAt(sourcePos, sourceSide.getOpposite(), sinkPos, sinkSide.getOpposite(), count, sourceTank);
        double delay = (double) moved / (double) rate - 0.05;
        if (delay > 0) context.pause(delay);
        return result(moved > 0, moved);
    }

    @Callback(doc = "function():number -- Returns the fluid transfer rate in liters per second.")
    default Object[] getFluidTransferRate(Context context, Arguments args) {
        return result(fluidTransferRate());
    }
}
