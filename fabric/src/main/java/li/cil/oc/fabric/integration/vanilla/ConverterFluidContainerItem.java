package li.cil.oc.fabric.integration.vanilla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.fabric.util.FabricFluidHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class ConverterFluidContainerItem implements Converter {
  @Override
  public void convert(Object value, Map<Object, Object> output) {
    if (value instanceof ItemStack stack) {
      var storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
      if (storage == null) return;
      var handler = new FabricFluidHandler(storage);
      int tanks = handler.getTanks();
      if (tanks <= 0) return;
      int capacity = 0;
      for (int i = 0; i < tanks; i++) {
        capacity += handler.getTankCapacity(i);
      }
      output.put("capacity", capacity);
      if (tanks > 1) {
        List<Object> tankData = new ArrayList<>(tanks);
        for (int i = 0; i < tanks; i++) {
          Map<Object, Object> tank = new HashMap<>();
          tank.put("capacity", handler.getTankCapacity(i));
          FluidStack fluid = handler.getFluidInTank(i);
          if (fluid == null || fluid.isEmpty()) {
            tank.put("amount", 0);
          } else {
            new ConverterFluidStack().convert(fluid, tank);
          }
          tankData.add(tank);
        }
        output.put("fluid", tankData);
      } else {
        FluidStack fluid = handler.getFluidInTank(0);
        output.put("fluid", fluid != null ? fluid : FluidStack.EMPTY);
      }
    }
  }
}
