package li.cil.oc.core.impl.server.component.traits;

import java.util.List;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;


public interface InventoryWorldControl extends InventoryAware, WorldAware, SideRestricted {
  @Override
  Player fakePlayer();

  default li.cil.oc.core.impl.util.InventoryUtils.InventorySource inventorySourceAtSide(BlockPosition pos, Direction side) {
    return InventoryUtils.inventorySourceAt(pos, side);
  }

  @Callback(doc = "function(side:number[, fuzzy:boolean=false]):boolean -- Compare the block on the specified side with the one in the selected slot. Returns true if equal.")
  default Object[] compare(Context context, Arguments args) {
    Direction side = checkSideForAction(args, 0);
    ItemStack stack = stackInSlot(selectedSlot());
    if (stack != null && stack.getItem() instanceof BlockItem item) {
      BlockPosition blockPos = position().offset(side);
      BlockState state = level().getBlockState(blockPos.toBlockPos());
      boolean fuzzy = args.optBoolean(1, false);
      boolean idMatches = item.getBlock() == state.getBlock();
      boolean subTypeMatches = fuzzy || stateMatchesItemVariant(stack, item, state);
      return ResultWrapper.result(idMatches && subTypeMatches);
    }
    return ResultWrapper.result(false);
  }

  static boolean stateMatchesItemVariant(ItemStack stack, BlockItem item, BlockState state) {
    var properties = stack.get(DataComponents.BLOCK_STATE);
    if (properties == null || properties.isEmpty()) return true;
    return properties.apply(item.getBlock().defaultBlockState()).equals(state);
  }

  @Callback(doc = "function(side:number[, count:number=64]):boolean -- Drops items from the selected slot towards the specified side.")
  default Object[] drop(Context context, Arguments args) {
    Direction facing = checkSideForAction(args, 0);
    int count = ExtendedArguments.optItemCount(args, 1, 64);
    ItemStack stack = inventory().getItem(selectedSlot());
    if (!stack.isEmpty()) {
      BlockPosition blockPos = position().offset(facing);
      InventoryUtils.InventorySource source = inventorySourceAtSide(blockPos, facing.getOpposite());
      net.minecraft.world.Container inv = source != null ? source.inventory() : null;
      if (inv != null && mayInteract(source)) {
        if (!InventoryUtils.insertIntoInventory(stack, inv, facing.getOpposite(), count, false)) {
          return ResultWrapper.result(false, "inventory full");
        } else if (stack.isEmpty()) {
          inventory().setItem(selectedSlot(), ItemStack.EMPTY);
        } else {
          inventory().setChanged();
        }
      } else {
        ItemStack dropped = inventory().removeItem(selectedSlot(), count);
        if (!dropped.isEmpty()) {
          java.util.function.Predicate<net.minecraft.world.entity.item.ItemEntity> tossValidator = null;
          if (fakePlayer() instanceof li.cil.oc.core.impl.server.agent.AgentPlayer agentPlayer) {
            tossValidator = agentPlayer::mayTossItem;
          }
          ItemEntity entity = InventoryUtils.spawnStackInWorld(position(), dropped, facing, tossValidator);
          if (entity == null) {
            fakePlayer().getInventory().add(dropped);
          }
        }
      }
      context.pause(OCSettings.get().dropDelay);
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
    InventoryUtils.InventorySource source = inventorySourceAtSide(blockPos, facing.getOpposite());
    net.minecraft.world.Container inv = source != null ? source.inventory() : null;
    if (inv != null && mayInteract(source)) {
      extracted = InventoryUtils.extractAnyFromInventory(
        (stack, simulate) -> InventoryUtils.insertIntoInventory(stack, this.inventory(), null, 64, simulate, insertionSlots()),
        inv, facing.getOpposite(), count);
    }
    if (extracted <= 0) {
      extracted = suckFromItems(facing);
    }
    if (extracted <= 0) {
      return ResultWrapper.result(false);
    } else {
      context.pause(OCSettings.get().suckDelay);
      return ResultWrapper.result(extracted);
    }
  }

  default void onSuckCollect(ItemEntity entity) {
    if (WorldAction.checkItemPickup(fakePlayer(), entity)) {
      return;
    }
    if (InventoryUtils.insertIntoInventory(entity.getItem(), inventory(), null, 64, false)) {
      InventoryUtils.playPickupSound(position());
    }
    if (entity.getItem().isEmpty()) {
      entity.discard();
    }
  }
}
