package li.cil.oc.fabric.integration.vanilla;

import java.util.Map;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.MapUtils;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("unused")
public final class ConverterFluidStack implements Converter {
    public static FluidStack parse(Map<?, ?> args) {
        String name = MapUtils.getString(args, "name");
        if (name == null) throw new IllegalArgumentException("fluid name expected");
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(name));
        Integer amount = MapUtils.getInt(args, "amount");
        return new FluidStack(name, amount != null ? amount : 0);
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof FluidStack(String fluidName, int amount, boolean hasTag)) {
            output.put("amount", amount);
            output.put("hasTag", hasTag);
            if (!fluidName.isEmpty()) {
                var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidName));
                if (OCSettings.get().insertIdsInConverters) {
                    output.put("id", BuiltInRegistries.FLUID.getId(fluid));
                }
                output.put("name", fluidName);
                output.put("label", FluidVariantAttributes.getName(FluidVariant.of(fluid)).getString());
            }
        }
    }
}
