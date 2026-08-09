package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.world.item.ItemStack;


public interface InventoryControl extends InventoryAware {
    @Callback(doc = "function():number -- The size of this device's internal inventory.")
    default Object[] inventorySize(Context context, Arguments args) {
        return ResultWrapper.result(inventory().getContainerSize());
    }

    @Callback(doc = "function([slot:number]):number -- Get the currently selected slot; set the selected slot if specified.")
    default Object[] select(Context context, Arguments args) {
        int slot = optSlot(args, 0);
        if (slot != selectedSlot()) {
            selectedSlot_$eq(slot);
        }
        return ResultWrapper.result(selectedSlot() + 1);
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Get the number of items in the specified slot, otherwise in the selected slot.")
    default Object[] count(Context context, Arguments args) {
        int slot = optSlot(args, 0);
        ItemStack stack = stackInSlot(slot);
        return ResultWrapper.result(stack != null ? stack.getCount() : 0);
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Get the remaining space in the specified slot, otherwise in the selected slot.")
    default Object[] space(Context context, Arguments args) {
        int slot = optSlot(args, 0);
        ItemStack stack = stackInSlot(slot);
        if (stack != null) {
            return ResultWrapper.result(Math.min(inventory().getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount());
        }
        return ResultWrapper.result(inventory().getMaxStackSize());
    }

    @Callback(doc = "function(otherSlot:number[, checkNBT:boolean=false]):boolean -- Compare the contents of the selected slot to the contents of the specified slot.")
    default Object[] compareTo(Context context, Arguments args) {
        int slot = ExtendedArguments.checkSlot(args, inventory(), 0);
        ItemStack stackA = stackInSlot(selectedSlot());
        ItemStack stackB = stackInSlot(slot);
        boolean eq;
        if (stackA != null && stackB != null) {
            eq = InventoryUtils.haveSameItemType(stackA, stackB, args.optBoolean(1, false));
        } else {
            eq = stackA == null && stackB == null;
        }
        return ResultWrapper.result(eq);
    }

    @Callback(doc = "function(toSlot:number[, amount:number]):boolean -- Move up to the specified amount of items from the selected slot into the specified slot.")
    default Object[] transferTo(Context context, Arguments args) {
        int slot = ExtendedArguments.checkSlot(args, inventory(), 0);
        int count = ExtendedArguments.optItemCount(args, 1, 64);
        if (slot == selectedSlot() || count == 0) {
            return ResultWrapper.result(true);
        }
        ItemStack from = stackInSlot(selectedSlot());
        ItemStack to = stackInSlot(slot);
        if (from != null && to != null) {
            if (InventoryUtils.haveSameItemType(from, to, true)) {
                int space = Math.min(inventory().getMaxStackSize(), to.getMaxStackSize()) - to.getCount();
                int amount = Math.min(count, Math.min(space, from.getCount()));
                if (amount > 0) {
                    from.shrink(amount);
                    to.grow(amount);
                    assert from.getCount() >= 0;
                    if (from.isEmpty()) {
                        inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                    }
                    inventory().setChanged();
                    return ResultWrapper.result(true);
                }
                return ResultWrapper.result(false);
            } else if (count >= from.getCount()) {
                inventory().setItem(slot, from);
                inventory().setItem(selectedSlot(), to);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(false);
        } else if (from != null) {
            inventory().setItem(slot, inventory().removeItem(selectedSlot(), count));
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false);
    }
}
