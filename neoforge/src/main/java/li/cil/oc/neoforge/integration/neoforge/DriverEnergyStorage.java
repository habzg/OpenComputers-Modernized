package li.cil.oc.neoforge.integration.neoforge;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

@SuppressWarnings("unused")
public final class DriverEnergyStorage extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return BlockEntity.class;
    }

    @Override
    public boolean worksWith(final Level world, final int x, final int y, final int z, final Direction side) {
        var pos = new BlockPos(x, y, z);
        return world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final int x, final int y, final int z, final Direction side) {
        var pos = new BlockPos(x, y, z);
        var storage = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        if (storage == null) return null;
        return new Environment(storage);
    }

    public static final class Environment extends li.cil.oc.api.prefab.ManagedEnvironment implements NamedBlock {
        private final IEnergyStorage storage;

        public Environment(final IEnergyStorage storage) {
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
            return ResultWrapper.result(storage.getEnergyStored());
        }

        @Callback(doc = "function():number -- Returns the maximum amount of stored energy on the connected side.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.getMaxEnergyStored());
        }

        @Callback(doc = "function():boolean -- Returns whether this component can have energy extracted from the connected side.")
        public Object[] canExtract(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.canExtract());
        }

        @Callback(doc = "function():boolean -- Returns whether this component can receive energy on the connected side.")
        public Object[] canReceive(final Context context, final Arguments args) {
            return ResultWrapper.result(storage.canReceive());
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return null;
        }
    }
}
