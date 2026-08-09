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
import li.cil.oc.core.server.component.traits.NetworkAware;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import static li.cil.oc.core.util.ResultWrapper.result;

public interface WorldFluidContainerAnalytics extends WorldAware, SideRestricted, NetworkAware {
    @Callback(doc = "function(side:number, slot:number):number -- Get the capacity of the fluid container in the specified slot.")
    default Object[] getContainerCapacityInSlot(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> {
                ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
                return withFluidInfo(stack, (fluid, capacity) -> result(capacity));
            });
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the level of the fluid container in the specified slot.")
    default Object[] getContainerLevelInSlot(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> {
                ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
                return withFluidInfo(stack, (fluid, capacity) -> result(fluid.amount()));
            });
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number, slot:number):table -- Get a description of the fluid in the container in the specified slot.")
    default Object[] getFluidInContainerInSlot(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> {
                ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
                return withFluidInfo(stack, (fluid, capacity) -> result(fluid));
            });
        }
        return result(null, "not enabled in config");
    }

    private Object[] withFluidInfo(ItemStack stack, BiFunction<FluidStack, Integer, Object[]> f) {
        if (!stack.isEmpty()) {
            FluidHandler handler = FluidUtils.fluidHandlerIn(stack);
            if (handler != null) {
                FluidStack fluid = handler.getFluidInTank(0);
                int capacity = handler.getTankCapacity(0);
                return f.apply(fluid != null && !fluid.isEmpty() ? fluid : null, capacity);
            }
        }
        return result(null, "item is not a fluid container");
    }

    private Object[] withInventory(Direction side, Function<Container, Object[]> f) {
        Container inv = InventoryUtils.inventoryAt(position().offset(side));
        if (inv != null && inv.stillValid(fakePlayer()) && mayInteract(position().offset(side), side.getOpposite())) {
            return f.apply(inv);
        }
        return result(null, "no inventory");
    }
}
