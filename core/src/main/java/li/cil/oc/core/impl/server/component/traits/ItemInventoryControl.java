package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface ItemInventoryControl extends InventoryAware {
    @Callback(doc = "function(slot:number):number -- The size of an item inventory in the specified slot.")
    default Object[] getItemInventorySize(Context context, Arguments args) {
        return withItemInventory(ExtendedArguments.checkSlot(args, inventory(), 0), itemInventory -> ResultWrapper.result(itemInventory.getContainerSize()));
    }

    @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Drops an item into the specified slot in the item inventory.")
    default Object[] dropIntoItemInventory(Context context, Arguments args) {
        return withItemInventory(ExtendedArguments.checkSlot(args, inventory(), 0), itemInventory -> {
            int slot = ExtendedArguments.checkSlot(args, itemInventory, 1);
            int count = ExtendedArguments.optItemCount(args, 2, 64);
            return ResultWrapper.result(InventoryUtils.extractAnyFromInventory(
                    stack -> InventoryUtils.insertIntoInventorySlot(stack, itemInventory, null, slot, count),
                    inventory(), null, count));
        });
    }

    @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Sucks an item out of the specified slot in the item inventory.")
    default Object[] suckFromItemInventory(Context context, Arguments args) {
        return withItemInventory(ExtendedArguments.checkSlot(args, inventory(), 0), itemInventory -> {
            int slot = ExtendedArguments.checkSlot(args, itemInventory, 1);
            int count = ExtendedArguments.optItemCount(args, 2, 64);
            return ResultWrapper.result(InventoryUtils.extractFromInventorySlot(
                    extractedStack -> {
                        for (int s : insertionSlots()) {
                            if (InventoryUtils.insertIntoInventorySlot(extractedStack, inventory(), null, s, 64)) break;
                        }
                    },
                    itemInventory, null, slot, count));
        });
    }

    default Object[] withItemInventory(int slot, java.util.function.Function<Container, Object[]> f) {
        ItemStack stack = inventory().getItem(slot);
        Container inv = li.cil.oc.api.Driver.inventoryFor(stack, fakePlayer());
        if (inv != null) return f.apply(inv);
        return ResultWrapper.result(0, "no item inventory");
    }
}
