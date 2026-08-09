package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTankInfo;
import net.minecraft.core.Direction;

import static li.cil.oc.core.util.ResultWrapper.result;

public interface WorldTankAnalytics extends WorldAware, SideRestricted {
    @Callback(doc = "function(side:number [, tank:number]):number -- Get the amount of fluid in the tank on the specified side.")
    default Object[] getTankLevel(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var handler = FluidUtils.fluidHandlerAt(position().offset(facing));
        if (handler != null) {
            FluidTankInfo info = optTankInfo(args, handler, 1, null);
            if (info != null)
                return result(info.fluid() != null && !info.fluid().isEmpty() ? info.fluid().amount() : 0);
            int sum = 0;
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fs = handler.getFluidInTank(i);
                sum += (fs != null && !fs.isEmpty()) ? fs.amount() : 0;
            }
            return result(sum);
        }
        return result(null, "no tank");
    }

    @Callback(doc = "function(side:number [, tank:number]):number -- Get the capacity of the tank on the specified side.")
    default Object[] getTankCapacity(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var handler = FluidUtils.fluidHandlerAt(position().offset(facing));
        if (handler != null) {
            FluidTankInfo info = optTankInfo(args, handler, 1, null);
            if (info != null) return result(info.capacity());
            int max = 0;
            for (int i = 0; i < handler.getTanks(); i++) {
                max = Math.max(max, handler.getTankCapacity(i));
            }
            return result(max);
        }
        return result(null, "no tank");
    }

    @Callback(doc = "function(side:number [, tank:number]):table -- Get a description of the fluid in the tank on the specified side.")
    default Object[] getFluidInTank(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            var handler = FluidUtils.fluidHandlerAt(position().offset(facing));
            if (handler != null) {
                FluidTankInfo info = optTankInfo(args, handler, 1, null);
                if (info != null) return result(info);
                FluidTankInfo[] tanks = new FluidTankInfo[handler.getTanks()];
                for (int i = 0; i < tanks.length; i++) {
                    tanks[i] = new FluidTankInfo(handler.getFluidInTank(i), handler.getTankCapacity(i));
                }
                return result((Object) tanks);
            }
            return result(null, "no tank");
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number):number -- Get the number of tanks available on the specified side.")
    default Object[] getTankCount(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var handler = FluidUtils.fluidHandlerAt(position().offset(facing));
        if (handler != null) return result(handler.getTanks());
        return result(null, "no tank");
    }

    default FluidTankInfo optTankInfo(Arguments args, FluidHandler handler, int n, FluidTankInfo defaultVal) {
        if (args.count() <= n || args.checkAny(n) == null) return defaultVal;
        int tank = args.checkInteger(n) - 1;
        int tanks = handler.getTanks();
        if (tank < 0 || tank >= tanks) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return new FluidTankInfo(handler.getFluidInTank(tank), handler.getTankCapacity(tank));
    }
}
