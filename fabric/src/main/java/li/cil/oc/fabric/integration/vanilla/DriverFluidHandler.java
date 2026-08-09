package li.cil.oc.fabric.integration.vanilla;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.fabric.util.FabricFluidHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        if (!world.isLoaded(pos)) return false;
        var storage = FluidStorage.SIDED.find(world, pos, side);
        return storage != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level level, final BlockPos pos, final Direction side) {
        var storage = FluidStorage.SIDED.find(level, pos, side);
        return new Environment(new FabricFluidHandler(storage));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<FluidHandler> {
        public Environment(final FluidHandler handler) {
          super(handler, "fluid_handler");
        }

        @Callback(doc = "function():table -- Get some information about the tank accessible from the specified side.")
        public Object[] getTankInfo(final Context context, final Arguments args) {
            var tankInfo = new java.util.ArrayList<java.util.Map<String, Object>>();
            int tanks = this.getBlockEntity().getTanks();
            for (int i = 0; i < tanks; i++) {
                var info = new java.util.HashMap<String, Object>();
                var fluidStack = this.getBlockEntity().getFluidInTank(i);
                info.put("amount", fluidStack.amount());
                if (!fluidStack.isEmpty()) {
                    var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidStack.fluidName()));
                    info.put("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
                    info.put("name", FluidVariantAttributes.getName(FluidVariant.of(fluid)).getString());
                }
                info.put("capacity", this.getBlockEntity().getTankCapacity(i));
                tankInfo.add(info);
            }
            return new Object[]{tankInfo};
        }
    }
}
