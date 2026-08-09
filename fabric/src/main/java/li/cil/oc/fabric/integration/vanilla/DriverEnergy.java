package li.cil.oc.fabric.integration.vanilla;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.util.Power;
import li.cil.oc.core.util.ResultWrapper;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import team.reborn.energy.api.EnergyStorage;

@SuppressWarnings("unused")
public final class DriverEnergy extends DriverSidedBlockEntity {
    @Override
    public boolean isGeneric() {
        return true;
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return BlockEntity.class;
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        if (!world.isLoaded(pos)) return false;
        return EnergyStorage.SIDED.find(world, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        var storage = EnergyStorage.SIDED.find(world, pos, side);
        if (storage == null) return null;
        return new Environment(storage);
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
        private final EnergyStorage storage;

        public Environment(final EnergyStorage storage) {
            this.storage = storage;
            setNode(Network.newNode(this, Visibility.Network).withComponent("energy_device").create());
        }

        @Override
        public String preferredName() {
            return "energy_device";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Returns the amount of stored energy on the connected side.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.getAmount());
        }

        @Callback(doc = "function():number -- Returns the maximum amount of stored energy on the connected side.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.getCapacity());
        }

        @Callback(doc = "function():boolean -- Returns whether this component can have energy extracted from the connected side.")
        public Object[] canExtract(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.supportsExtraction());
        }

        @Callback(doc = "function():boolean -- Returns whether this component can receive energy on the connected side.")
        public Object[] canReceive(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.supportsInsertion());
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static boolean canCharge(final ItemStack stack) {
        if (stack.getItem() instanceof Chargeable) return false;
        EnergyStorage storage = ContainerItemContext.withConstant(stack).find(EnergyStorage.ITEM);
        return storage != null && storage.supportsInsertion();
    }

    @SuppressWarnings("unused")
    public static double charge(final ItemStack stack, final double amount, final boolean simulate) {
        if (stack.getItem() instanceof Chargeable) return amount;

        var container = new SimpleContainer(1);
        container.setItem(0, stack.copy());

        var inventoryStorage = InventoryStorage.of(container, null);
        var context = ContainerItemContext.ofSingleSlot(inventoryStorage.getSlot(0));

        EnergyStorage storage = context.find(EnergyStorage.ITEM);
        if (storage == null) return amount;

        try (var transaction = Transaction.openOuter()) {
            long inserted = storage.insert(Power.toRF(amount), transaction);
            if (simulate) {
                transaction.abort();
            } else {
                transaction.commit();
                stack.applyComponents(container.getItem(0).getComponents());
            }
            return amount - Power.fromRF((int) inserted);
        }
    }
}
