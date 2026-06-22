package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@SuppressWarnings("unused")
public final class DriverFluidTank extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IFluidHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level level, final int x, final int y, final int z, final Direction side) {
        return new Environment((IFluidHandler) level.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IFluidHandler> {
        public Environment(final IFluidHandler handler) {
            super(handler, "fluid_tank");
        }

        @Callback(doc = "function():table -- Get some information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            var tankInfo = new java.util.ArrayList<java.util.Map<String, Object>>();
            int tanks = getTileEntity().getTanks();
            for (int i = 0; i < tanks; i++) {
                var info = new java.util.HashMap<String, Object>();
                var fluidStack = getTileEntity().getFluidInTank(i);
                info.put("amount", fluidStack.getAmount());
                if (!fluidStack.isEmpty()) {
                    info.put("fluid", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString());
                    info.put("name", fluidStack.getFluid().getFluidType().getDescription().getString());
                }
                info.put("capacity", getTileEntity().getTankCapacity(i));
                tankInfo.add(info);
            }
            return new Object[]{tankInfo};
        }
    }
}
