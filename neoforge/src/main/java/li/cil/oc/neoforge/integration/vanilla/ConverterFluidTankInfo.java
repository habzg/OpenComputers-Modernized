package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.util.FluidTankInfo;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterFluidTankInfo implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof FluidTank tankInfo) {
            output.put("capacity", tankInfo.getCapacity());
            FluidStack fluid = tankInfo.getFluid();
            if (!fluid.isEmpty()) {
                new ConverterFluidStack().convert(fluid, output);
            } else {
                output.put("amount", 0);
            }
        } else if (value instanceof FluidTankInfo(li.cil.oc.core.util.FluidStack fs, int capacity)) {
            output.put("capacity", capacity);
            if (fs != null && !fs.isEmpty()) {
                output.put("name", fs.fluidName());
                output.put("amount", fs.amount());
            } else {
                output.put("amount", 0);
            }
        }
    }
}
