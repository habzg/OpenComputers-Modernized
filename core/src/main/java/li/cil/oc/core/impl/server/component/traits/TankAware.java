package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;

public interface TankAware {
    MultiTank tank();

    int selectedTank();

    void selectedTank_$eq(int value);

    default int optTank(Arguments args, int ignoredN) {
        if (args.count() > 0 && args.checkAny(0) != null)
            return ExtendedArguments.checkTank(args, tank(), 0);
        return selectedTank();
    }

    default FluidTank getTank(int index) {
        if (index >= 0 && index < tank().tankCount())
            return FluidUtils.tankFrom(tank(), index);
        return null;
    }

    default FluidStack fluidInTank(int index) {
        FluidTank t = getTank(index);
        return t != null ? t.getFluid() : null;
    }

    default boolean haveSameFluidType(FluidStack stackA, FluidStack stackB) {
        return stackA != null && stackA.hasSameFluid(stackB);
    }
}
