package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@SuppressWarnings("unused")
public final class DriverFluidHandler extends DriverSidedBlockEntity {
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
        return world.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level level, final BlockPos pos, final Direction side) {
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (handler == null) return null;
        return new Environment(handler);
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IFluidHandler> {
        public Environment(final IFluidHandler handler) {
            super(handler, "fluid_handler");
        }

        @Callback(doc = "function():table -- Get some information about the tank accessible from the specified side.")
        public Object[] getTankInfo(final Context context, final Arguments args) {
            var tankInfo = new java.util.ArrayList<java.util.Map<String, Object>>();
            int tanks = getBlockEntity().getTanks();
            for (int i = 0; i < tanks; i++) {
                var info = new java.util.HashMap<String, Object>();
                var fluidStack = getBlockEntity().getFluidInTank(i);
                info.put("amount", fluidStack.getAmount());
                if (!fluidStack.isEmpty()) {
                    info.put("fluid", BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString());
                    info.put("name", fluidStack.getFluid().getFluidType().getDescription().getString());
                }
                info.put("capacity", getBlockEntity().getTankCapacity(i));
                tankInfo.add(info);
            }
            return new Object[]{tankInfo};
        }
    }
}
