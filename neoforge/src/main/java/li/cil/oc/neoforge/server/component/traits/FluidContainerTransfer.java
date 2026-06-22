package li.cil.oc.neoforge.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.server.component.traits.FluidContainerTransferBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.neoforge.util.FluidContainerUtils;
import li.cil.oc.neoforge.util.FluidUtilsOriginal;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;


import java.util.function.BiFunction;
import java.util.function.Consumer;

import static li.cil.oc.core.util.ResultWrapper.result;

public interface FluidContainerTransfer extends FluidContainerTransferBase {

    @Callback(doc = "function(tankSide:number, inventorySide:number, inventorySlot:number[, count:number[, sourceTank:number[, outputSide:number[, outputSlot:number]]]]):boolean, number -- Transfer some fluid from the tank to the container.")
    default Object[] transferFluidFromTankToContainer(Context context, Arguments args) {
        Direction tankSide = checkSideForAction(args, 0);
        BlockPosition tankPos = position().offset(tankSide);
        Direction inventorySide = checkSideForAction(args, 1);
        int count = ExtendedArguments.optFluidCount(args, 3, Integer.MAX_VALUE);
        int sourceTank = args.optInteger(4, -1);
        Direction outputSide = args.count() > 5 ? checkSideForAction(args, 5) : inventorySide;
        String reason = onTransferContents();
        if (reason != null) return result(null, reason);
        return withInventory(inventorySide, inventory -> {
            int inventorySlot = ExtendedArguments.checkSlot(args, inventory, 2);
            return withInventory(outputSide, output -> {
                Integer outputSlot = args.count() > 6 ? ExtendedArguments.checkSlot(args, output, 6) : null;
                return withReplayableMove(
                        FluidUtilsOriginal.fluidHandlerAt(tankPos),
                        FluidContainerUtils.fluidHandlerIn(inventory, inventorySlot),
                        (replayableTank, replayableContainer) -> FluidUtilsOriginal.doTransfer(replayableTank, tankSide, replayableContainer, null, count, sourceTank),
                        (tank, container, tankReplay, containerReplay) -> {
                            containerReplay.accept(container);
                            ItemStack containerResult = FluidContainerUtils.getContainerResult(container);
                            if (syncResult(inventory, inventorySide, inventorySlot, output, outputSide, outputSlot, containerResult)) {
                                tankReplay.accept(tank);
                                return true;
                            }
                            return false;
                        }
                );
            });
        });
    }

    @Callback(doc = "function(inventorySide:number, inventorySlot:number, tankSide:number[, count:number[, outputSide:number[, outputSlot:number]]]):boolean, number -- Transfer some fluid from the container to the tank.")
    default Object[] transferFluidFromContainerToTank(Context context, Arguments args) {
        Direction inventorySide = checkSideForAction(args, 0);
        Direction tankSide = checkSideForAction(args, 2);
        BlockPosition tankPos = position().offset(tankSide);
        int count = ExtendedArguments.optFluidCount(args, 3, Integer.MAX_VALUE);
        Direction outputSide = args.count() > 4 ? checkSideForAction(args, 4) : inventorySide;
        String reason = onTransferContents();
        if (reason != null) return result(null, reason);
        return withInventory(inventorySide, inventory -> {
            int inventorySlot = ExtendedArguments.checkSlot(args, inventory, 1);
            return withInventory(outputSide, output -> {
                Integer outputSlot = args.count() > 5 ? ExtendedArguments.checkSlot(args, output, 5) : null;
                return withReplayableMove(
                        FluidContainerUtils.fluidHandlerIn(inventory, inventorySlot),
                        FluidUtilsOriginal.fluidHandlerAt(tankPos),
                        (replayableContainer, replayableTank) -> FluidUtilsOriginal.doTransfer(replayableContainer, null, replayableTank, tankSide, count),
                        (container, tank, containerReplay, tankReplay) -> {
                            containerReplay.accept(container);
                            ItemStack result = FluidContainerUtils.getContainerResult(container);
                            if (syncResult(inventory, inventorySide, inventorySlot, output, outputSide, outputSlot, result)) {
                                tankReplay.accept(tank);
                                return true;
                            }
                            return false;
                        }
                );
            });
        });
    }

    @Callback(doc = "function(sourceSide:number, sourceSlot:number, sinkSide:number, sinkSlot:number[, count:number, ...]):boolean, number -- Transfer some fluid between containers.")
    default Object[] transferFluidBetweenContainers(Context context, Arguments args) {
        Direction sourceSide = checkSideForAction(args, 0);
        Direction sinkSide = checkSideForAction(args, 2);
        int count = ExtendedArguments.optFluidCount(args, 4, Integer.MAX_VALUE);
        Direction sourceOutputSide = args.count() > 5 ? checkSideForAction(args, 5) : sourceSide;
        Direction sinkOutputSide = args.count() > 6 ? checkSideForAction(args, 6) : sinkSide;
        String reason = onTransferContents();
        if (reason != null) return result(null, reason);
        return withInventory(sourceSide, source -> {
            int sourceSlot = ExtendedArguments.checkSlot(args, source, 1);
            return withInventory(sinkSide, sink -> {
                int sinkSlot = ExtendedArguments.checkSlot(args, sink, 3);
                return withInventory(sourceOutputSide, sourceOutput -> {
                    Integer sourceOutputSlot = args.count() > 7 ? ExtendedArguments.checkSlot(args, sourceOutput, 7) : null;
                    return withInventory(sinkOutputSide, sinkOutput -> {
                        Integer sinkOutputSlot = args.count() > 8 ? ExtendedArguments.checkSlot(args, sinkOutput, 8) : null;
                        return withMove(
                                FluidContainerUtils.fluidHandlerIn(source, sourceSlot),
                                FluidContainerUtils.fluidHandlerIn(sink, sinkSlot),
                                (sourceContainer, sinkContainer) -> FluidUtilsOriginal.doTransfer(sourceContainer, null, sinkContainer, null, count),
                                (sourceContainer, sinkContainer) -> {
                                    ItemStack sourceResult = FluidContainerUtils.getContainerResult(sourceContainer);
                                    ItemStack sinkResult = FluidContainerUtils.getContainerResult(sinkContainer);
                                    if (syncResult(source, sourceSide, sourceSlot, sourceOutput, sourceOutputSide, sourceOutputSlot, sourceResult, true)
                                            && syncResult(sink, sinkSide, sinkSlot, sinkOutput, sinkOutputSide, sinkOutputSlot, sinkResult, true)) {
                                        syncResult(source, sourceSide, sourceSlot, sourceOutput, sourceOutputSide, sourceOutputSlot, sourceResult);
                                        syncResult(sink, sinkSide, sinkSlot, sinkOutput, sinkOutputSide, sinkOutputSlot, sinkResult);
                                        return true;
                                    }
                                    return false;
                                }
                        );
                    });
                });
            });
        });
    }

    private boolean syncResult(Container inventory, Direction inventorySide, int inventorySlot, Container output, Direction outputSide, Integer outputSlot, ItemStack result) {
        return syncResult(inventory, inventorySide, inventorySlot, output, outputSide, outputSlot, result, false);
    }

    private boolean syncResult(Container inventory, Direction inventorySide, int inventorySlot, Container output, Direction outputSide, Integer outputSlot, ItemStack result, boolean simulate) {
        ItemStack stack = simulate ? result.copy() : result;
        java.util.function.Function<Boolean, Boolean> decrStackSizeIfInserted = inserted -> {
            if (inserted != null && inserted && !simulate) {
                inventory.removeItem(inventorySlot, 1);
            }
            return inserted;
        };
        java.util.function.Function<java.util.function.Supplier<Boolean>, Boolean> replaceOr = f -> {
            if (inventorySide == outputSide && (outputSlot != null ? outputSlot : inventorySlot) == inventorySlot && inventory.getItem(inventorySlot).getCount() == 1) {
                if (!simulate) inventory.setItem(inventorySlot, stack);
                return true;
            }
            return f.get();
        };
        if (outputSlot != null) {
            return replaceOr.apply(() -> decrStackSizeIfInserted.apply(InventoryUtils.insertIntoInventorySlot(stack, output, outputSide.getOpposite(), outputSlot, 1, simulate)));
        } else {
            return replaceOr.apply(() -> decrStackSizeIfInserted.apply(InventoryUtils.insertIntoInventory(stack, output, outputSide.getOpposite(), 1, simulate)));
        }
    }

    private Object[] withReplayableMove(IFluidHandler handlerA, IFluidHandler handlerB,
                                        BiFunction<IFluidHandler, IFluidHandler, Integer> moveFunc,
                                        QuadPredicate<IFluidHandler, IFluidHandler, Consumer<IFluidHandler>, Consumer<IFluidHandler>> afterMovedFunc) {
        if (handlerA == null || handlerB == null) return result(false, 0);
        IFluidHandler replayableA = FluidContainerUtils.replayableFluidHandler(handlerA);
        IFluidHandler replayableB = FluidContainerUtils.replayableFluidHandler(handlerB);
        int m = moveFunc.apply(replayableA, replayableB);
        if (m != 0 && afterMovedFunc.test(handlerA, handlerB,
                h -> FluidContainerUtils.replay(replayableA, h),
                h -> FluidContainerUtils.replay(replayableB, h))) return result(true, m);
        return result(false, 0);
    }

    private Object[] withMove(IFluidHandler handlerA, IFluidHandler handlerB,
                              BiFunction<IFluidHandler, IFluidHandler, Integer> moveFunc,
                              java.util.function.BiPredicate<IFluidHandler, IFluidHandler> afterMovedFunc) {
        if (handlerA == null || handlerB == null) return result(false, 0);
        int m = moveFunc.apply(handlerA, handlerB);
        if (m != 0 && afterMovedFunc.test(handlerA, handlerB)) return result(true, m);
        return result(false, 0);
    }

    private Object[] withInventory(Direction side, java.util.function.Function<Container, Object[]> f) {
        Container inv = InventoryUtils.inventoryAt(position().offset(side));
        if (inv != null && inv.stillValid(fakePlayer()) && mayInteract(position().offset(side), side.getOpposite())) {
            return f.apply(inv);
        }
        return result(null, "no inventory");
    }

    @FunctionalInterface
    interface QuadPredicate<A, B, C, D> {
        boolean test(A a, B b, C c, D d);
    }
}
