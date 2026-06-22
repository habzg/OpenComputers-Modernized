package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.ItemStackArrayValue;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.server.component.traits.NetworkAware;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import static li.cil.oc.core.util.ResultWrapper.result;

public interface WorldInventoryAnalytics extends WorldAware, SideRestricted, NetworkAware {
    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on the specified side.")
    default Object[] getInventorySize(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> result(inventory.getContainerSize()));
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get number of items in the specified slot.")
    default Object[] getSlotStackSize(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
            return result(stack.getCount());
        });
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the maximum number of items in the specified slot.")
    default Object[] getSlotMaxStackSize(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
            return result(stack.getMaxStackSize());
        });
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number[, checkNBT:boolean=false]):boolean -- Get whether the items in the two specified slots are of the same type.")
    default Object[] compareStacks(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            ItemStack stackA = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
            ItemStack stackB = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 2));
            return result(stackA == stackB || InventoryUtils.haveSameItemType(stackA, stackB, args.optBoolean(3, false)));
        });
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item with one in the database.")
    default Object[] compareStackToDatabase(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            int slot = ExtendedArguments.checkSlot(args, inventory, 1);
            String dbAddress = args.checkString(2);
            ItemStack stack = inventory.getItem(slot);
            return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
                int dbSlot = ExtendedArguments.checkSlot(args, database.data(), 3);
                ItemStack dbStack = database.getStackInSlot(dbSlot);
                return result(InventoryUtils.haveSameItemType(stack, dbStack, args.optBoolean(4, false)));
            });
        });
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number):boolean -- Get whether the items are equivalent (shared OreDictionary IDs).")
    default Object[] areStacksEquivalent(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        return withInventory(facing, inventory -> {
            ItemStack stackA = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
            ItemStack stackB = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 2));
            boolean eq = stackA == stackB;
            if (!eq) {
                eq = stackA.getItem() == stackB.getItem();
            }
            return result(eq);
        });
    }

    @Callback(doc = "function(side:number, slot:number, label:string):boolean -- Change the display name of the stack.")
    default Object[] setStackDisplayName(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        String label = args.checkString(2).trim();
        return withInventory(facing, inventory -> {
            ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
            if (stack.getCount() > 0) {
                if (!label.isEmpty())
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(label));
                else if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
                    stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                return result(true);
            }
            return result(false);
        });
    }

    @Callback(doc = "function(side:number, slot:number):table -- Get a description of the stack.")
    default Object[] getStackInSlot(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> result(inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1))));
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number):userdata -- Get a description of all stacks in the inventory.")
    default Object[] getAllStacks(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            return withInventory(facing, inventory -> {
                ItemStack[] stacks = new ItemStack[inventory.getContainerSize()];
                for (int i = 0; i < inventory.getContainerSize(); i++)
                    stacks[i] = inventory.getItem(i);
                return result(new ItemStackArrayValue(stacks));
            });
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number):string -- Get the name of the inventory on the specified side.")
    default Object[] getInventoryName(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            Direction facing = checkSideForAction(args, 0);
            return withInventorySource(facing, is -> {
                if (is instanceof InventoryUtils.BlockInventorySource) {
                    BlockPosition pos = ((InventoryUtils.BlockInventorySource) is).position();
                    var blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
                    if (pos.level() != null && pos.level().hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                        Block block = pos.level().getBlockState(blockPos).getBlock();
                        return result(block.getDescriptionId());
                    }
                    return result(null, "Unknown");
                } else if (is instanceof InventoryUtils.EntityInventorySource) {
                    return result(EntityType.getKey(((InventoryUtils.EntityInventorySource) is).entity().getType()).toString());
                }
                return result(null, "Unknown");
            });
        }
        return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the database.")
    default Object[] store(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        String dbAddress = args.checkString(2);
        return withInventory(facing, inventory -> {
            ItemStack stack = inventory.getItem(ExtendedArguments.checkSlot(args, inventory, 1));
            return DatabaseAccess.withDatabase(node(), dbAddress, database -> {
                int dbSlot = ExtendedArguments.checkSlot(args, database.data(), 3);
                boolean nonEmpty = database.getStackInSlot(dbSlot) != null;
                database.setStackInSlot(dbSlot, stack.copy());
                return result(nonEmpty);
            });
        });
    }

    private boolean mayInteract(Direction side, InventoryUtils.InventorySource f) {
        if (!f.inventory().stillValid(fakePlayer())) return false;
        if (f instanceof InventoryUtils.BlockInventorySource) {
            return mayInteract(((InventoryUtils.BlockInventorySource) f).position(), side.getOpposite());
        }
        return true;
    }

    private Object[] withInventorySource(Direction side, java.util.function.Function<InventoryUtils.InventorySource, Object[]> f) {
        InventoryUtils.InventorySource is = InventoryUtils.inventorySourceAt(position().offset(side));
        if (is != null && mayInteract(side, is)) return f.apply(is);
        return result(null, "no inventory");
    }

    private Object[] withInventory(Direction side, java.util.function.Function<Container, Object[]> f) {
        return withInventorySource(side, is -> f.apply(is.inventory()));
    }
}
