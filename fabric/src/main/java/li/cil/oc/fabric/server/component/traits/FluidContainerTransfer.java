package li.cil.oc.fabric.server.component.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.server.component.traits.FluidContainerTransferBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.fabric.util.FabricFluidHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
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
                        fluidHandlerAt(tankPos),
                        fluidHandlerIn(inventory, inventorySlot),
                        (replayableTank, replayableContainer) -> doTransfer(replayableTank, replayableContainer, count, sourceTank),
                        (tank, container, tankReplay, containerReplay) -> {
                            containerReplay.accept(container);
                            ItemStack containerResult = getContainerResult(container);
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
                        fluidHandlerIn(inventory, inventorySlot),
                        fluidHandlerAt(tankPos),
                        (replayableContainer, replayableTank) -> doTransfer(replayableContainer, replayableTank, count),
                        (container, tank, containerReplay, tankReplay) -> {
                            containerReplay.accept(container);
                            ItemStack result = getContainerResult(container);
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
                                fluidHandlerIn(source, sourceSlot),
                                fluidHandlerIn(sink, sinkSlot),
                                (sourceContainer, sinkContainer) -> doTransfer(sourceContainer, sinkContainer, count),
                                (sourceContainer, sinkContainer) -> {
                                    ItemStack sourceResult = getContainerResult(sourceContainer);
                                    ItemStack sinkResult = getContainerResult(sinkContainer);
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

    private Object[] withReplayableMove(FluidHandler handlerA, FluidHandler handlerB,
                                         BiFunction<FluidHandler, FluidHandler, Integer> moveFunc,
                                         QuadPredicate<FluidHandler, FluidHandler, Consumer<FluidHandler>, Consumer<FluidHandler>> afterMovedFunc) {
        if (handlerA == null || handlerB == null) return result(false, 0);
        ReplayableFluidHandler replayableA = new ReplayableFluidHandler(handlerA, true);
        ReplayableFluidHandler replayableB = new ReplayableFluidHandler(handlerB, true);
        int m = moveFunc.apply(replayableA, replayableB);
        if (m != 0 && afterMovedFunc.test(handlerA, handlerB,
                replayableA::replay,
                replayableB::replay)) return result(true, m);
        return result(false, 0);
    }

    private Object[] withMove(FluidHandler handlerA, FluidHandler handlerB,
                               BiFunction<FluidHandler, FluidHandler, Integer> moveFunc,
                               java.util.function.BiPredicate<FluidHandler, FluidHandler> afterMovedFunc) {
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

    private static FluidHandler fluidHandlerAt(BlockPosition pos) {
        return FluidUtils.fluidHandlerAt(pos);
    }

    private static ContainerFluidHandler fluidHandlerIn(Container inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            SimpleContainer container = new SimpleContainer(1);
            container.setItem(0, copy);
            var inventoryStorage = InventoryStorage.of(container, null);
            var context = ContainerItemContext.ofSingleSlot(inventoryStorage.getSlot(0));
            Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
            if (storage != null) {
                FluidHandler handler = new FabricFluidHandler(storage);
                return new ContainerFluidHandler(handler, container);
            }
        }
        return null;
    }

    private static ItemStack getContainerResult(FluidHandler handler) {
        if (handler instanceof ContainerFluidHandler cfh) {
            return cfh.getResult();
        }
        return null;
    }

    private static int doTransfer(FluidHandler source, FluidHandler sink, int limit) {
        return doTransfer(source, sink, limit, -1);
    }

    private static int doTransfer(FluidHandler source, FluidHandler sink, int limit, int sourceTank) {
        int tanks = source.getTanks();
        FluidStack srcFluid = (sourceTank < 0 || tanks <= sourceTank) ? null : source.getFluidInTank(sourceTank);
        boolean nullFluid = srcFluid == null || srcFluid.isEmpty();
        FluidStack drained;
        if (nullFluid) {
            drained = source.drain(limit, true);
        } else {
            drained = source.drain(srcFluid.copyWithAmount(limit), true);
        }
        if (!drained.isEmpty()) {
            int filled = sink.fill(drained, true);
            if (nullFluid) {
                sink.fill(source.drain(filled, false), false);
            } else {
                srcFluid = srcFluid.copyWithAmount(filled);
                sink.fill(source.drain(srcFluid, false), false);
            }
            return filled;
        }
        return 0;
    }

    class ContainerFluidHandler implements FluidHandler {
        final FluidHandler delegate;
        final SimpleContainer container;
        boolean dirty;

        ContainerFluidHandler(FluidHandler delegate, SimpleContainer container) {
            this.delegate = delegate;
            this.container = container;
            this.dirty = false;
        }

        ItemStack getResult() {
            return dirty ? container.getItem(0) : null;
        }

        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return delegate.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return delegate.getTankCapacity(tank);
        }

        @Override
        public int fill(FluidStack resource, boolean simulate) {
            dirty = true;
            return delegate.fill(resource, simulate);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            dirty = true;
            return delegate.drain(resource, simulate);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean simulate) {
            dirty = true;
            return delegate.drain(maxDrain, simulate);
        }
    }

    class ReplayableFluidHandler implements FluidHandler {
        final FluidHandler delegate;
        final boolean simulate;
        final List<Consumer<FluidHandler>> actions = new ArrayList<>();

        ReplayableFluidHandler(FluidHandler handler, boolean simulate) {
            this.delegate = handler;
            this.simulate = simulate;
        }

        void replay(FluidHandler target) {
            for (Consumer<FluidHandler> action : actions) {
                action.accept(target);
            }
        }

        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return delegate.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return delegate.getTankCapacity(tank);
        }

        @Override
        public int fill(FluidStack resource, boolean simulate) {
            boolean realSimulate = simulate || this.simulate;
            actions.add(h -> h.fill(resource, realSimulate));
            return delegate.fill(resource, realSimulate);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            boolean realSimulate = simulate || this.simulate;
            actions.add(h -> h.drain(resource, realSimulate));
            return delegate.drain(resource, realSimulate);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean simulate) {
            boolean realSimulate = simulate || this.simulate;
            actions.add(h -> h.drain(maxDrain, realSimulate));
            return delegate.drain(maxDrain, realSimulate);
        }
    }
}
