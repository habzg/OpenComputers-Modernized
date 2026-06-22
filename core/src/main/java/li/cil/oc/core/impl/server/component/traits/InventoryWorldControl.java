package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;


public interface InventoryWorldControl extends InventoryAware, WorldAware, SideRestricted {
    @Override
    Player fakePlayer();

    @Callback(doc = "function(side:number[, fuzzy:boolean=false]):boolean -- Compare the block on the specified side with the one in the selected slot.")
    default Object[] compare(Context context, Arguments args) {
        Direction side = checkSideForAction(args, 0);
        ItemStack stack = stackInSlot(selectedSlot());
        if (stack != null && stack.getItem() instanceof BlockItem item) {
            BlockPosition blockPos = position().offset(side);
            BlockState state = level().getBlockState(blockPos.toBlockPos());
            boolean fuzzy = args.optBoolean(1, false);
            boolean idMatches = item.getBlock() == state.getBlock();
            boolean subTypeMatches = fuzzy || state == item.getBlock().defaultBlockState();
            return ResultWrapper.result(idMatches && subTypeMatches);
        }
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(side:number[, count:number=64]):boolean -- Drops items from the selected slot towards the specified side.")
    default Object[] drop(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        int count = ExtendedArguments.optItemCount(args, 1, 64);
        ItemStack stack = inventory().getItem(selectedSlot());
        if (!stack.isEmpty()) {
            BlockPosition blockPos = position().offset(facing);
            net.minecraft.world.Container inv = InventoryUtils.inventoryAt(blockPos);
            if (inv != null && inv.stillValid(fakePlayer()) && mayInteract(blockPos, facing.getOpposite())) {
                if (!InventoryUtils.insertIntoInventory(stack, inv, facing.getOpposite(), count)) {
                    return ResultWrapper.result(false, "inventory full");
                } else if (stack.isEmpty()) {
                    inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                } else {
                    inventory().setChanged();
                }
            } else {
                ItemStack dropped = inventory().removeItem(selectedSlot(), count);
                if (!dropped.isEmpty()) {
                    ItemEntity entity = InventoryUtils.spawnStackInWorld(position(), dropped, facing, null);
                    if (entity == null) {
                        fakePlayer().getInventory().add(dropped);
                    }
                }
            }
            context.pause(Settings.get().dropDelay);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false);
    }

    default int suckFromItems(Direction facing) {
        return collectFromItems(entitiesOnSide(facing, ItemEntity.class));
    }

    default int collectFromItems(List<ItemEntity> items) {
        for (ItemEntity entity : items) {
            if (!entity.isRemoved() && !entity.hasPickUpDelay()) {
                ItemStack stack = entity.getItem();
                int size = stack.getCount();
                onSuckCollect(entity);
                if (stack.getCount() < size) return size - stack.getCount();
                else if (entity.isRemoved()) return size;
            }
        }
        return 0;
    }

    @Callback(doc = "function(side:number[, count:number=64]):boolean -- Suck up items from the specified side.")
    default Object[] suck(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        int count = ExtendedArguments.optItemCount(args, 1, 64);
        BlockPosition blockPos = position().offset(facing);
        int extracted = 0;
        net.minecraft.world.Container inv = InventoryUtils.inventoryAt(blockPos);
        if (inv != null && inv.stillValid(fakePlayer()) && mayInteract(blockPos, facing.getOpposite())) {
            extracted = InventoryUtils.extractAnyFromInventory(
                    stack -> {
                        for (int slot : insertionSlots()) {
                            if (InventoryUtils.insertIntoInventorySlot(stack, this.inventory(), null, slot, 64)) break;
                        }
                    },
                    inv, facing.getOpposite(), count);
        }
        if (extracted <= 0) {
            extracted = suckFromItems(facing);
        }
        if (extracted <= 0) {
            return ResultWrapper.result(false);
        } else {
            context.pause(Settings.get().suckDelay);
            return ResultWrapper.result(extracted);
        }
    }

    default void onSuckCollect(ItemEntity entity) {
        entity.playerTouch(fakePlayer());
    }
}
