package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;

public interface TankWorldControl extends TankAware, WorldAware, SideRestricted {
    @Callback(doc = "function(side:number [, tank:number]):boolean -- Compare the fluid in the selected tank with the fluid in the specified tank on the specified side. Returns true if equal.")
    default Object[] compareFluid(Context context, Arguments args) {
        Direction side = checkSideForAction(args, 0);
        FluidStack stack = fluidInTank(selectedTank());
        if (stack != null) {
            FluidHandler handler = FluidUtils.fluidHandlerAt(position().offset(side), side.getOpposite());
            if (handler != null) {
                if (args.count() > 1 && args.checkAny(1) != null) {
                    int tank = args.checkInteger(1) - 1;
                    if (tank < 0 || tank >= handler.getTanks())
                        throw new IllegalArgumentException("invalid tank index");
                    FluidStack fs = handler.getFluidInTank(tank);
                    return ResultWrapper.result(fs != null && fs.hasSameFluid(stack));
                }
                for (int i = 0; i < handler.getTanks(); i++) {
                    FluidStack fs = handler.getFluidInTank(i);
                    if (fs != null && fs.hasSameFluid(stack)) return ResultWrapper.result(true);
                }
                return ResultWrapper.result(false);
            }
            return ResultWrapper.result(false);
        }
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(side:number[, amount:number=1000]):boolean, number or string -- Drains the specified amount of fluid from the specified side. Returns the amount drained, or an error message.")
    default Object[] drain(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        int count = Math.max(ExtendedArguments.optFluidCount(args, 1, 1000), 0);
        FluidTank tank = getTank(selectedTank());
        if (tank == null) return ResultWrapper.result(null, "no tank selected");
        int space = tank.getSpace();
        int amount = Math.min(count, space);
        if (count >= 1 && amount <= 0) return ResultWrapper.result(null, "tank is full");
        FluidHandler handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (handler == null) return ResultWrapper.result(null, "incompatible or no fluid");
        FluidStack existing = tank.getFluid();
        FluidStack drained = existing != null && !existing.isEmpty()
                ? handler.drain(existing.copyWithAmount(amount), false)
                : handler.drain(amount, false);
        if ((drained != null && !drained.isEmpty()) || amount == 0) {
            int filled = tank.fill(drained, false);
            return ResultWrapper.result(true, filled);
        }
        return ResultWrapper.result(null, "incompatible or no fluid");
    }

    @Callback(doc = "function(side:number[, amount:number=1000]):boolean, number or string -- Eject the specified amount of fluid to the specified side. Returns the amount ejected or an error message.")
    default Object[] fill(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        int count = Math.max(ExtendedArguments.optFluidCount(args, 1, 1000), 0);
        FluidTank tank = getTank(selectedTank());
        if (tank == null) return ResultWrapper.result(null, "no tank selected");
        int amount = Math.min(count, tank.getFluidAmount());
        if (count >= 1 && amount <= 0) return ResultWrapper.result(null, "tank is empty");
        FluidHandler handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (handler == null) return ResultWrapper.result(null, "no space");
        FluidStack existing = tank.getFluid();
        int filled = handler.fill(existing != null ? existing.copyWithAmount(amount) : new FluidStack("", 0), false);
        if (filled > 0 || amount == 0) {
            tank.drain(filled, false);
            return ResultWrapper.result(true, filled);
        }
        return ResultWrapper.result(null, "incompatible or no fluid");
    }
}
