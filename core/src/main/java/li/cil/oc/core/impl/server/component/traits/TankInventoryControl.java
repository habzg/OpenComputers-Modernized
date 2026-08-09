package li.cil.oc.core.impl.server.component.traits;

import java.util.function.BiFunction;
import java.util.function.Function;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import static li.cil.oc.core.util.ResultWrapper.result;

public interface TankInventoryControl extends WorldAware, InventoryAware, TankAware {
    @Override
    Player fakePlayer();

    @Callback(doc = "function([slot:number]):number -- Get the amount of fluid in the tank item in the specified slot or the selected slot.")
    default Object[] getTankLevelInSlot(Context context, Arguments args) {
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> result(fluid.amount()));
    }

    @Callback(doc = "function([slot:number]):number -- Get the capacity of the tank item in the specified slot of the robot or the selected slot.")
    default Object[] getTankCapacityInSlot(Context context, Arguments args) {
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> result(capacity));
    }

    @Callback(doc = "function([slot:number]):table -- Get a description of the fluid in the tank item in the specified slot or the selected slot.")
    default Object[] getFluidInTankInSlot(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> result(fluid));
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function([tank:number]):table -- Get a description of the fluid in the tank in the specified slot or the selected slot.")
    default Object[] getFluidInInternalTank(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            FluidTank tank = getTank(optTank(args, 0));
            return result(tank != null ? tank.getFluid() : null);
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function([amount:number]):boolean -- Transfers fluid from a tank in the selected inventory slot to the selected tank.")
    default Object[] drain(Context context, Arguments args) {
        int amount = ExtendedArguments.optFluidCount(args, 0, 1000);
        FluidTank into = getTank(selectedTank());
        if (into == null) return result(null, "no tank");
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack.isEmpty()) return result(null, "nothing selected");
        FluidHandler fluidHandler = FluidUtils.fluidHandlerIn(stack);
        if (fluidHandler != null) {
            FluidStack drainable = fluidHandler.drain(amount, true);
            var remaining = stack.getItem().getCraftingRemainingItem();
            ItemStack container = remaining != null ? new ItemStack(remaining) : ItemStack.EMPTY;
            if (drainable == null || drainable.isEmpty())
                return result(null, "item is empty or not a fluid container");
            int transferred = into.fill(drainable, true);
            if (transferred <= 0)
                return result(null, "tank is full or incompatible fluid");
            into.fill(drainable.copyWithAmount(transferred), false);
            fluidHandler.drain(transferred, false);
            inventory().removeItem(selectedSlot(), 1);
            if (!container.isEmpty()) {
                for (int s : insertionSlots()) {
                    if (InventoryUtils.insertIntoInventorySlot(container, inventory(), null, s, 64)) break;
                }
                if (container.getCount() > 0) {
                    InventoryUtils.spawnStackInWorld(position(), container, null, null);
                }
            }
            return result(true, transferred);
        }
        return result(null, "item is empty or not a fluid container");
    }

    @Callback(doc = "function([amount:number]):boolean -- Transfers fluid from the selected tank to a tank in the selected inventory slot.")
    default Object[] fill(Context context, Arguments args) {
        int amount = ExtendedArguments.optFluidCount(args, 0, 1000);
        FluidTank from = getTank(selectedTank());
        if (from == null) return result(null, "no tank");
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack.isEmpty()) return result(null, "nothing selected");
        FluidHandler fluidHandler = FluidUtils.fluidHandlerIn(stack);
        if (fluidHandler != null) {
            FluidStack available = from.drain(amount, true);
            if (available == null || available.isEmpty())
                return result(null, "tank is empty");
            int transferred = fluidHandler.fill(available, true);
            if (transferred <= 0)
                return result(null, "item is full or incompatible fluid");
            ItemStack filledStack = FluidUtils.fillItem(stack, available.copyWithAmount(transferred));
            from.drain(transferred, false);
            if (filledStack != null) {
                inventory().removeItem(selectedSlot(), 1);
                for (int s : insertionSlots()) {
                    if (InventoryUtils.insertIntoInventorySlot(filledStack, inventory(), null, s, 64)) break;
                }
                if (filledStack.getCount() > 0) {
                    InventoryUtils.spawnStackInWorld(position(), filledStack, null, null);
                }
                return result(true, transferred);
            }
            return result(null, "item is full or incompatible fluid");
        }
        if (FluidUtils.isFluidContainer(stack)) {
            return result(null, "item is full or incompatible fluid");
        }
        return result(null, "item is not a fluid container");
    }

    default int optSlot(Arguments args, int n) {
        return ExtendedArguments.optSlot(args, inventory(), n, selectedSlot());
    }

    default Object[] withFluidInfo(int slot, BiFunction<FluidStack, Integer, Object[]> f) {
        ItemStack stack = inventory().getItem(slot);
        if (stack.isEmpty()) return null;
        Function<ItemStack, Object[]> fluidInfo = s -> {
            FluidHandler handler = FluidUtils.fluidHandlerIn(s);
            if (handler != null) {
                FluidStack fluid = handler.getFluidInTank(0);
                int capacity = handler.getTankCapacity(0);
                return new Object[]{fluid, capacity};
            }
            return null;
        };
        Object[] info = fluidInfo.apply(stack);
        if (info != null) {
            return f.apply((FluidStack) info[0], (Integer) info[1]);
        }
        return result(null, "item is not a fluid container");
    }
}
