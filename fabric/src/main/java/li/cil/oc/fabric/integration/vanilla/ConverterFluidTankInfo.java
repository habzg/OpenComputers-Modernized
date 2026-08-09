package li.cil.oc.fabric.integration.vanilla;

import java.util.Map;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;
import li.cil.oc.core.util.FluidTankInfo;

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
        } else if (value instanceof FluidTankInfo(FluidStack fs, int capacity)) {
            output.put("capacity", capacity);
            if (fs != null && !fs.isEmpty()) {
                new ConverterFluidStack().convert(fs, output);
            } else {
                output.put("amount", 0);
                output.put("hasTag", false);
            }
        }
    }
}
