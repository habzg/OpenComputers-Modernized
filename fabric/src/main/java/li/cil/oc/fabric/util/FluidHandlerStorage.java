package li.cil.oc.fabric.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record FluidHandlerStorage(FluidHandler handler) implements Storage<FluidVariant> {
    private static final long MB_TO_DROPLETS = FluidConstants.BUCKET / 1000;

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        int mb = (int) Math.min(maxAmount / MB_TO_DROPLETS, Integer.MAX_VALUE);
        if (mb == 0) return 0;
        FluidStack stack = toCore(resource, mb);
        int inserted = handler.fill(stack, false);
        if (inserted > 0) {
            transaction.addCloseCallback((t, result) -> {
                if (!result.wasCommitted()) {
                    handler.drain(toCore(resource, inserted), false);
                }
            });
        }
        return inserted * MB_TO_DROPLETS;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        int mb = (int) Math.min(maxAmount / MB_TO_DROPLETS, Integer.MAX_VALUE);
        if (mb == 0) return 0;
        FluidStack toDrain = toCore(resource, mb);
        FluidStack drained = handler.drain(toDrain, false);
        if (drained.isEmpty()) return 0;
        transaction.addCloseCallback((t, result) -> {
            if (!result.wasCommitted()) {
                handler.fill(drained, false);
            }
        });
        return drained.amount() * MB_TO_DROPLETS;
    }

    @Override
    public @NotNull Iterator<StorageView<FluidVariant>> iterator() {
        int tanks = handler.getTanks();
        List<StorageView<FluidVariant>> views = new ArrayList<>(tanks);
        for (int i = 0; i < tanks; i++) {
            FluidStack fluid = handler.getFluidInTank(i);
            int capacity = handler.getTankCapacity(i);
            views.add(new FluidHandlerView(fluid, capacity));
        }
        return views.iterator();
    }

    private static FluidStack toCore(FluidVariant variant, int amount) {
        return new FluidStack(BuiltInRegistries.FLUID.getKey(variant.getFluid()).toString(), amount, !variant.getComponents().isEmpty());
    }

    private record FluidHandlerView(FluidStack fluid, int capacity) implements StorageView<FluidVariant> {
        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public boolean isResourceBlank() {
            return fluid.isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            if (fluid.isEmpty()) return FluidVariant.blank();
            return FluidVariant.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluid.fluidName())));
        }

        @Override
        public long getAmount() {
            return fluid.isEmpty() ? 0 : fluid.amount() * MB_TO_DROPLETS;
        }

        @Override
        public long getCapacity() {
            return capacity * MB_TO_DROPLETS;
        }
    }
}
