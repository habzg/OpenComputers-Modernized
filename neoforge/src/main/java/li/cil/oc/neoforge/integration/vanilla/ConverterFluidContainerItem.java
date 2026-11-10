package li.cil.oc.neoforge.integration.vanilla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings("unused")
public final class ConverterFluidContainerItem implements Converter {
  @Override
  public void convert(Object value, Map<Object, Object> output) {
    if (value instanceof ItemStack stack) {
      var cap = stack.getCapability(Capabilities.FluidHandler.ITEM);
      if (cap == null) return;
      int tanks = cap.getTanks();
      if (tanks <= 0) return;
      int capacity = 0;
      for (int i = 0; i < tanks; i++) {
        capacity += cap.getTankCapacity(i);
      }
      output.put("capacity", capacity);
      if (tanks > 1) {
        List<Object> tankData = new ArrayList<>(tanks);
        for (int i = 0; i < tanks; i++) {
          Map<Object, Object> tank = new HashMap<>();
          tank.put("capacity", cap.getTankCapacity(i));
          FluidStack fluid = cap.getFluidInTank(i);
          if (fluid.isEmpty()) {
            tank.put("amount", 0);
          } else {
            new ConverterFluidStack().convert(fluid, tank);
          }
          tankData.add(tank);
        }
        output.put("fluid", tankData);
      } else {
        FluidStack fluid = cap.getFluidInTank(0);
        output.put("fluid", fluid);
      }
    }
  }
}
