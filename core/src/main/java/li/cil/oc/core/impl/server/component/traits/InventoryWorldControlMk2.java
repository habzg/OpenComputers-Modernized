package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc.core.util.ResultWrapper.result;

public interface InventoryWorldControlMk2 extends InventoryAware, WorldAware, SideRestricted {
    @Override
    Player fakePlayer();

    @Callback(doc = "function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Drops the selected item stack into the specified slot of an inventory.")
    default Object[] dropIntoSlot(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        int count = ExtendedArguments.optItemCount(args, 2, 64);
        Direction fromSide = ExtendedArguments.optSideAny(args, 3, facing.getOpposite());
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack.getCount() > 0) {
            return withInventory(position().offset(facing), fromSide, inv -> {
                int slot = ExtendedArguments.checkSlot(args, inv, 1);
                if (!InventoryUtils.insertIntoInventorySlot(stack, inv, fromSide, slot, count)) {
                    return result(false, "inventory full/invalid slot");
                } else if (stack.getCount() == 0) {
                    inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                } else {
                    inventory().setChanged();
                }
                context.pause(Settings.get().dropDelay);
                return result(true);
            });
        }
        return result(false);
    }

    @Callback(doc = "function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Sucks items from the specified slot of an inventory.")
    default Object[] suckFromSlot(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        int count = ExtendedArguments.optItemCount(args, 2, 64);
        Direction fromSide = ExtendedArguments.optSideAny(args, 3, facing.getOpposite());
        return withInventory(position().offset(facing), fromSide, inv -> {
            int slot = ExtendedArguments.checkSlot(args, inv, 1);
            int extracted = InventoryUtils.extractFromInventorySlot(
                    extractedStack -> {
                        for (int s : insertionSlots()) {
                            if (InventoryUtils.insertIntoInventorySlot(extractedStack, this.inventory(), null, s, 64))
                                break;
                        }
                    },
                    inv, fromSide, slot, count);
            if (extracted > 0) {
                context.pause(Settings.get().suckDelay);
                return result(extracted);
            }
            return result(false);
        });
    }

    default Object[] withInventory(BlockPosition blockPos, Direction fromSide, java.util.function.Function<Container, Object[]> f) {
        Container inv = InventoryUtils.inventoryAt(blockPos);
        if (inv != null && inv.stillValid(fakePlayer()) && mayInteract(blockPos, fromSide)) {
            return f.apply(inv);
        }
        return result(null, "no inventory");
    }
}
