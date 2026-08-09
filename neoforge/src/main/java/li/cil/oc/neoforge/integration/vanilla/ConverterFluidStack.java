package li.cil.oc.neoforge.integration.vanilla;

import java.util.Map;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.MapUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings("unused")
public final class ConverterFluidStack implements Converter {
    public static FluidStack parse(Map<?, ?> args) {
        String name = MapUtils.getString(args, "name");
        if (name == null) throw new IllegalArgumentException("fluid name expected");
        var fluid = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(name));
        Integer amount = MapUtils.getInt(args, "amount");
        return new FluidStack(fluid, amount != null ? amount : 0);
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof FluidStack stack) {
            var fluid = stack.getFluid();
            var fluidType = fluid.getFluidType();
            output.put("amount", stack.getAmount());
            output.put("hasTag", !stack.getComponents().isEmpty());
            if (!stack.isEmpty()) {
                if (OCSettings.get().insertIdsInConverters) {
                    output.put("id", BuiltInRegistries.FLUID.getId(fluid));
                }
                output.put("name", BuiltInRegistries.FLUID.getKey(fluid).toString());
                output.put("label", fluidType.getDescription().getString());
            }
        } else if (value instanceof li.cil.oc.core.util.FluidStack stack) {
            output.put("amount", stack.amount());
            output.put("hasTag", stack.hasTag());
            if (!stack.isEmpty()) {
                var fluid = BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(stack.fluidName()));
                if (OCSettings.get().insertIdsInConverters) {
                    output.put("id", BuiltInRegistries.FLUID.getId(fluid));
                }
                output.put("name", stack.fluidName());
                output.put("label", fluid.getFluidType().getDescription().getString());
            }
        }
    }
}
