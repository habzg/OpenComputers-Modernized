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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public final class DriverFluidTank extends DriverSidedBlockEntity {
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
    if (storage == null) return null;
    return new Environment(level, pos, side, new FabricFluidHandler(storage));
  }

  public static final class Environment extends ManagedBlockEntityEnvironment<FluidHandler> {
    private final Level level;
    private final BlockPos pos;
    private final Direction side;

    public Environment(Level level, BlockPos pos, Direction side, final FluidHandler handler) {
      super(handler, "fluid_tank");
      this.level = level;
      this.pos = pos;
      this.side = side;
    }

    private FluidHandler resolve() {
      var storage = FluidStorage.SIDED.find(level, pos, side);
      return storage != null ? new FabricFluidHandler(storage) : null;
    }

    @Callback(doc = "function():table -- Get some information about this tank.")
    public Object[] getInfo(final Context context, final Arguments args) {
      java.util.Map<Object, Object> info = new java.util.HashMap<>();
      var handler = resolve();
      if (handler != null) {
        var fluidStack = handler.getFluidInTank(0);
        if (!fluidStack.isEmpty()) {
          new ConverterFluidStack().convert(fluidStack, info);
        } else {
          info.put("amount", 0);
        }
        info.put("capacity", handler.getTankCapacity(0));
      } else {
        info.put("amount", 0);
        info.put("capacity", 0);
      }
      return new Object[]{info};
    }
  }
}
