package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.server.component.traits.NetworkAware;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc.core.util.ResultWrapper.result;

public interface InventoryAnalytics extends InventoryAware, NetworkAware {
    @Callback(doc = "function([slot:number]):table -- Get a description of the stack in the specified slot or the selected slot.")
    default Object[] getStackInInternalSlot(Context context, Arguments args) {
        if (OCSettings.get().allowItemStackInspection) {
            int slot = optSlot(args, 0);
            return result(inventory().getItem(slot));
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(otherSlot:number):boolean -- Get whether the stack in the selected slot is equivalent to the item in the specified slot (have shared OreDictionary IDs).")
    default Object[] isEquivalentTo(Context context, Arguments args) {
        int slot = ExtendedArguments.checkSlot(args, inventory(), 0);
        ItemStack stackA = stackInSlot(selectedSlot());
        ItemStack stackB = stackInSlot(slot);
        boolean eq;
        if (stackA != null && !stackA.isEmpty() && stackB != null && !stackB.isEmpty()) {
            eq = stackA.is(stackB.getItem());
            if (!eq) {
                eq = stackA.getTags().anyMatch(stackB::is);
            }
        } else {
            eq = (stackA == null || stackA.isEmpty()) && (stackB == null || stackB.isEmpty());
        }
        return result(eq);
    }

    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.")
    default Object[] storeInternal(Context context, Arguments args) {
        int localSlot = ExtendedArguments.checkSlot(args, inventory(), 0);
        String dbAddress = args.checkString(1);
        ItemStack localStack = inventory().getItem(localSlot);
        return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
            int dbSlot = ExtendedArguments.checkSlot(args, database.data(), 2);
            boolean nonEmpty = !database.getStackInSlot(dbSlot).isEmpty();
            database.setStackInSlot(dbSlot, localStack.copy());
            return result(nonEmpty);
        });
    }

    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot with one in the database with the specified address.")
    default Object[] compareToDatabase(Context context, Arguments args) {
        int localSlot = ExtendedArguments.checkSlot(args, inventory(), 0);
        String dbAddress = args.checkString(1);
        ItemStack localStack = inventory().getItem(localSlot);
        return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
            int dbSlot = ExtendedArguments.checkSlot(args, database.data(), 2);
            ItemStack dbStack = database.getStackInSlot(dbSlot);
            return result(InventoryUtils.haveSameItemType(localStack, dbStack, args.optBoolean(3, false)));
        });
    }
}
