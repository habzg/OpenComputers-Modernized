package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;
import li.cil.oc.core.util.ResultWrapper;

public interface TankControl extends TankAware {
    @Callback(doc = "function():number -- The number of tanks installed in the device.")
    default Object[] tankCount(Context context, Arguments args) {
        return ResultWrapper.result(tank().tankCount());
    }

    @Callback(doc = "function([index:number]):number -- Select a tank and/or get the number of the currently selected tank.")
    default Object[] selectTank(Context context, Arguments args) {
        if (args.count() > 0 && args.checkAny(0) != null) {
            selectedTank_$eq(ExtendedArguments.checkTank(args, tank(), 0));
        }
        return ResultWrapper.result(selectedTank() + 1);
    }

    @Callback(direct = true, doc = "function([index:number]):number -- Get the fluid amount in the specified or selected tank.")
    default Object[] tankLevel(Context context, Arguments args) {
        int index;
        if (args.count() > 0 && args.checkAny(0) != null) index = ExtendedArguments.checkTank(args, tank(), 0);
        else index = selectedTank();
        FluidStack fluid = fluidInTank(index);
        return ResultWrapper.result(fluid != null ? fluid.amount() : 0);
    }

    @Callback(direct = true, doc = "function([index:number]):number -- Get the remaining fluid capacity in the specified or selected tank.")
    default Object[] tankSpace(Context context, Arguments args) {
        int index;
        if (args.count() > 0 && args.checkAny(0) != null) index = ExtendedArguments.checkTank(args, tank(), 0);
        else index = selectedTank();
        FluidTank tank = getTank(index);
        if (tank != null) return ResultWrapper.result(tank.getSpace());
        return ResultWrapper.result(0);
    }

    @Callback(doc = "function(index:number):boolean -- Compares the fluids in the selected and the specified tank. Returns true if equal.")
    default Object[] compareFluidTo(Context context, Arguments args) {
        int index = ExtendedArguments.checkTank(args, tank(), 0);
        FluidStack a = fluidInTank(selectedTank());
        FluidStack b = fluidInTank(index);
        boolean eq;
        if (a != null && b != null) eq = haveSameFluidType(a, b);
        else eq = a == null && b == null;
        return ResultWrapper.result(eq);
    }

    @Callback(doc = "function(index:number[, count:number=1000]):boolean -- Move the specified amount of fluid from the selected tank into the specified tank.")
    default Object[] transferFluidTo(Context context, Arguments args) {
        int index = ExtendedArguments.checkTank(args, tank(), 0);
        int count = ExtendedArguments.optFluidCount(args, 1, 1000);
        if (index == selectedTank() || count == 0) return ResultWrapper.result(true);
        FluidTank from = getTank(selectedTank());
        FluidTank to = getTank(index);
        if (from != null && to != null) {
            FluidStack drained = from.drain(count, true);
            int transferred = to.fill(drained, false);
            if (transferred > 0) {
                from.drain(transferred, false);
                return ResultWrapper.result(true);
            } else if (count >= from.getFluidAmount() && to.getCapacity() >= from.getFluidAmount() && from.getCapacity() >= to.getFluidAmount()) {
                FluidStack tmp = to.drain(to.getFluidAmount(), false);
                to.fill(from.drain(from.getFluidAmount(), false), false);
                from.fill(tmp, false);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(null, "incompatible or no fluid");
        }
        return ResultWrapper.result(null, "invalid index");
    }
}
