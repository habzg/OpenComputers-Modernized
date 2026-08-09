package li.cil.oc.fabric.integration.vanilla;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.driver.Registry;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.fabric.util.FabricFluidHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class ConverterFluidContainerItem implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack) {
            var storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
            if (storage != null) {
                var handler = new FabricFluidHandler(storage);
                output.put("capacity", handler.getTankCapacity(0));
                FluidStack fluidStack = handler.getFluidInTank(0);
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
                    var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidStack.fluidName()));
                    if (OCSettings.get().insertIdsInConverters) {
                        output.put("fluid_id", BuiltInRegistries.FLUID.getId(fluid));
                    }
                    output.put("fluid_hasTag", fluidStack.hasTag());
                    output.put("fluid_name", BuiltInRegistries.FLUID.getKey(fluid).toString());
                    output.put("fluid_label", FluidVariantAttributes.getName(FluidVariant.of(fluid)).getString());
                }
            }
        }
    }
}
