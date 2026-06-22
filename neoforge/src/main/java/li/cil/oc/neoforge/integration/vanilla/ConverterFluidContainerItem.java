package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.server.driver.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterFluidContainerItem implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack) {
            var cap = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (cap != null) {
                output.put("capacity", cap.getTankCapacity(0));
                FluidStack fluidStack = cap.getFluidInTank(0);
                if (!fluidStack.isEmpty()) {
                    Object[] fluidData = Registry.INSTANCE.convert(new Object[]{fluidStack});
                    if (fluidData.length > 0) {
                        output.put("fluid", fluidData[0]);
                    }
                }
                if (!output.containsKey("fluid")) {
                    Map<Object, Object> fluidMap = new HashMap<>();
                    fluidMap.put("amount", 0);
                    output.put("fluid", fluidMap);
                }

                if (!fluidStack.isEmpty()) {
                    var fluid = fluidStack.getFluid();
                    var fluidType = fluid.getFluidType();
                    if (Settings.get().insertIdsInConverters) {
                        output.put("fluid_id", BuiltInRegistries.FLUID.getId(fluid));
                    }
                    output.put("fluid_hasTag", !fluidStack.getComponents().isEmpty());
                    output.put("fluid_name", fluidType.getDescriptionId());
                    output.put("fluid_label", fluidType.getDescription().getString());
                }
            }
        }
    }
}
