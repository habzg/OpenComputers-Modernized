package li.cil.oc.fabric.util;

import java.util.Collections;
import java.util.Iterator;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

public class FluidTransferHandler implements FluidUtils.FluidTransferHandler {
    @Override
    public FluidHandler fluidHandlerAt(BlockPosition position) {
        if (position.level() != null) {
            Level world = position.level();
            if (world.isLoaded(position.toBlockPos())) {
                for (Direction side : Direction.values()) {
                    Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, position.toBlockPos(), side);
                    if (storage != null) {
                        return new FabricFluidHandler(storage);
                    }
                }
                if (world.getBlockState(position.toBlockPos()).getBlock() instanceof LiquidBlock) {
                    return new FabricFluidHandler(new LiquidBlockStorage(world, position.toBlockPos()));
                }
            }
        }
        return null;
    }

    @Override
    public FluidHandler fluidHandlerIn(ItemStack stack) {
        if (!stack.isEmpty()) {
            var context = ContainerItemContext.withConstant(stack);
            Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
            if (storage != null) {
                return new FabricFluidHandler(storage);
            }
        }
        return null;
    }

    @Override
    public ItemStack fillItem(ItemStack stack, FluidStack resource) {
        if (stack.isEmpty()) return null;
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stack.copy());
        var inventoryStorage = InventoryStorage.of(container, null);
        var context = ContainerItemContext.ofSingleSlot(inventoryStorage.getSlot(0));
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage == null) return null;
        long droplets = resource.amount() * (FluidConstants.BUCKET / 1000);
        try (Transaction tx = Transaction.openOuter()) {
            long filled = storage.insert(FabricFluidHandler.toFabric(resource), droplets, tx);
            if (filled > 0) {
                tx.commit();
                return container.getItem(0);
            }
        }
        return null;
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var context = ContainerItemContext.withConstant(stack);
        return context.find(FluidStorage.ITEM) != null;
    }

    @Override
    public int transferBetweenFluidHandlers(FluidHandler source, Direction sourceSide, FluidHandler sink, Direction sinkSide, int limit, int sourceTank) {
        FabricStoragePair pair = unwrapPair(source, sink);
        if (pair == null) return 0;
        long limitDroplets = (long) limit * (FluidConstants.BUCKET / 1000);
        long moved = StorageUtil.move(pair.from(), pair.to(), fv -> {
            if (sourceTank < 0) return true;
            var fluid = source.getFluidInTank(sourceTank);
            return !fluid.isEmpty() && fv.getFluid() == BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluid.fluidName()));
        }, limitDroplets, null);
        return (int) (moved / (FluidConstants.BUCKET / 1000));
    }

    @Override
    public int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide, int limit, int sourceTank) {
        if (sourcePos.level() == null || sinkPos.level() == null) return 0;
        Level world = sourcePos.level();
        if (!world.isLoaded(sourcePos.toBlockPos()) || !world.isLoaded(sinkPos.toBlockPos())) return 0;
        Storage<FluidVariant> from = findStorage(world, sourcePos.toBlockPos(), sourceSide);
        Storage<FluidVariant> to = findStorage(world, sinkPos.toBlockPos(), sinkSide);
        if (from == null || to == null) return 0;
        FluidStack srcFluid = FluidStack.EMPTY;
        if (sourceTank >= 0) {
            FluidHandler srcHandler = new FabricFluidHandler(from);
            int tanks = srcHandler.getTanks();
            if (sourceTank < tanks) {
                srcFluid = srcHandler.getFluidInTank(sourceTank);
            }
        }
        boolean nullFluid = srcFluid.isEmpty();

        final FluidStack filterFluid = srcFluid;
        long limitDroplets = (long) limit * (FluidConstants.BUCKET / 1000);
        long moved = StorageUtil.move(from, to, fv -> {
            if (nullFluid) return true;
            net.minecraft.world.level.material.Fluid fluidType =
                    BuiltInRegistries.FLUID.get(ResourceLocation.parse(filterFluid.fluidName()));
            return fv.getFluid() == fluidType;
        }, limitDroplets, null);
        return (int) (moved / (FluidConstants.BUCKET / 1000));
    }

    private Storage<FluidVariant> findStorage(Level world, BlockPos pos, Direction side) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, pos, side);
        if (storage != null) return storage;
        if (world.getBlockState(pos).getBlock() instanceof LiquidBlock) {
            return new LiquidBlockStorage(world, pos);
        }
        return null;
    }

    @Override
    public FluidTank tankFrom(MultiTank multiTank, int index) {
        Object tank = multiTank.getFluidTank(index);
        if (tank instanceof FluidTank ft) return ft;
        if (tank instanceof Storage) {
            @SuppressWarnings("unchecked")
            Storage<FluidVariant> storage = (Storage<FluidVariant>) tank;
            return new FabricTankWrapper(storage);
        }
        return null;
    }

    @SuppressWarnings("unused")
    private record FabricStoragePair(Storage<FluidVariant> from, Storage<FluidVariant> to) {}

    private FabricStoragePair unwrapPair(FluidHandler source, FluidHandler sink) {
        Storage<FluidVariant> from = null;
        Storage<FluidVariant> to = null;
        if (source instanceof FabricFluidHandler(Storage<FluidVariant> delegate1)) from = delegate1;
        if (sink instanceof FabricFluidHandler(Storage<FluidVariant> delegate)) to = delegate;
        if (from == null || to == null) return null;
        return new FabricStoragePair(from, to);
    }

    private record FabricTankWrapper(Storage<FluidVariant> storage) implements FluidTank {
        private static final long MB_TO_DROPLETS = FluidConstants.BUCKET / 1000;

        @SuppressWarnings("unused")
        @Override
        public FluidStack getFluid() {
            try (Transaction tx = Transaction.openOuter()) {
                for (var view : storage) {
                    FluidVariant fluid = view.getResource();
                    if (!fluid.isBlank()) return FabricFluidHandler.fromFabric(fluid, view.getAmount());
                }
            }
            return FluidStack.EMPTY;
        }

        @SuppressWarnings("unused")
        @Override
        public int getCapacity() {
            long cap = 0;
            try (Transaction tx = Transaction.openOuter()) {
                for (var view : storage) cap += view.getCapacity();
            }
            return (int) (cap / MB_TO_DROPLETS);
        }

        @SuppressWarnings("unused")
        @Override
        public int getFluidAmount() {
            long amt = 0;
            try (Transaction tx = Transaction.openOuter()) {
                for (var view : storage) amt += view.getAmount();
            }
            return (int) (amt / MB_TO_DROPLETS);
        }

        @Override
        public int getSpace() {
            return getCapacity() - getFluidAmount();
        }

        @Override
        public int fill(FluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return 0;
            FluidVariant fluid = FabricFluidHandler.toFabric(resource);
            long droplets = resource.amount() * MB_TO_DROPLETS;
            try (Transaction tx = Transaction.openOuter()) {
                long filled = storage.insert(fluid, droplets, tx);
                if (!simulate) tx.commit();
                return (int) (filled / MB_TO_DROPLETS);
            }
        }

        @Override
        public FluidStack drain(int maxDrain, boolean simulate) {
            long droplets = maxDrain * MB_TO_DROPLETS;
            try (Transaction tx = Transaction.openOuter()) {
                for (var view : storage) {
                    FluidVariant fluid = view.getResource();
                    if (!fluid.isBlank()) {
                        long drained = storage.extract(fluid, droplets, tx);
                        if (!simulate) tx.commit();
                        return drained > 0 ? FabricFluidHandler.fromFabric(fluid, drained) : FluidStack.EMPTY;
                    }
                }
            }
            return FluidStack.EMPTY;
        }
    }

    private record LiquidBlockStorage(Level world, BlockPos pos) implements Storage<FluidVariant> {

        private FluidVariant getVariant() {
                FluidState state = world.getFluidState(pos);
                return state.isEmpty() ? FluidVariant.blank() : FluidVariant.of(state.getType());
            }

            @Override
            public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public boolean supportsInsertion() {
                return false;
            }

            @Override
            public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                if (resource.isBlank()) return 0;
                FluidVariant variant = getVariant();
                if (variant.isBlank() || !variant.equals(resource)) return 0;
                return Math.min(maxAmount, FluidConstants.BUCKET);
            }

            @Override
            public @NotNull Iterator<StorageView<FluidVariant>> iterator() {
                FluidVariant variant = getVariant();
                if (variant.isBlank()) {
                    return Collections.emptyIterator();
                }
                return Collections.<StorageView<FluidVariant>>singletonList(new LiquidBlockView(variant)).iterator();
            }

            private final class LiquidBlockView implements StorageView<FluidVariant> {
                private final FluidVariant variant;

                LiquidBlockView(FluidVariant variant) {
                    this.variant = variant;
                }

                @Override
                public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                    return LiquidBlockStorage.this.extract(resource, maxAmount, transaction);
                }

                @Override
                public boolean isResourceBlank() {
                    return variant.isBlank();
                }

                @Override
                public FluidVariant getResource() {
                    return variant;
                }

                @Override
                public long getAmount() {
                    return FluidConstants.BUCKET;
                }

                @Override
                public long getCapacity() {
                    return FluidConstants.BUCKET;
                }
            }
        }
}
