package li.cil.oc.core.impl.util;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public final class InventoryUtils {

    public static boolean haveSameItemType(ItemStack stackA, ItemStack stackB, boolean checkNBT) {
        return stackA != null && !stackA.isEmpty() && stackB != null && !stackB.isEmpty() &&
                stackA.getItem() == stackB.getItem() &&
                (!stackA.getItem().isDamageable(stackA) || stackA.getDamageValue() == stackB.getDamageValue()) &&
                (!checkNBT || ItemStack.isSameItemSameComponents(stackA, stackB));
    }

    public static InventorySource inventorySourceAt(BlockPosition position) {
        if (position.level() != null) {
            net.minecraft.world.level.Level world = position.level();
            if (world.isLoaded(position.toBlockPos())) {
                BlockEntity te = world.getBlockEntity(position.toBlockPos());
                if (te instanceof Container inv) {
                    return new BlockInventorySource(position, inv);
                }
            }
        }
        return null;
    }

    public static Container inventoryAt(BlockPosition position) {
        InventorySource source = inventorySourceAt(position);
        return source != null ? source.inventory() : null;
    }

    public static boolean insertIntoInventorySlot(ItemStack stack, Container inventory, Direction side, int slot, int limit, boolean simulate) {
        if (stack == null || stack.isEmpty() || limit <= 0) return false;

        boolean isSideValidForSlot = true;
        if (inventory instanceof WorldlyContainer sided && side != null) {
            isSideValidForSlot = sided.canPlaceItemThroughFace(slot, stack, side);
        }

        if (stack.getCount() > 0 && inventory.canPlaceItem(slot, stack) && isSideValidForSlot) {
            int maxStackSize = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
            ItemStack existing = inventory.getItem(slot);
            boolean shouldMerge = !existing.isEmpty() && existing.getCount() < maxStackSize && ItemStack.isSameItem(existing, stack) && ItemStack.isSameItemSameComponents(existing, stack);
            if (shouldMerge) {
                int space = maxStackSize - existing.getCount();
                int amount = Math.min(space, Math.min(stack.getCount(), limit));
                stack.shrink(amount);
                if (!simulate) {
                    existing.grow(amount);
                    inventory.setChanged();
                }
                return amount > 0;
            } else if (existing.isEmpty()) {
                int amount = Math.min(maxStackSize, Math.min(stack.getCount(), limit));
                ItemStack inserted = stack.split(amount);
                if (!simulate) {
                    inventory.setItem(slot, inserted);
                }
                return amount > 0;
            }
        }
        return false;
    }

    public static boolean insertIntoInventorySlot(ItemStack stack, Container inventory, Direction side, int slot, int limit) {
        return insertIntoInventorySlot(stack, inventory, side, slot, limit, false);
    }

    public static int extractFromInventorySlot(Consumer<ItemStack> consumer, Container inventory, Direction side, int slot, int limit) {
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty() || limit <= 0) return 0;

        if (inventory instanceof WorldlyContainer sided) {
            if (!sided.canTakeItemThroughFace(slot, stack, side)) return 0;
        }

        int maxStackSize = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
        int amount = Math.min(stack.getCount(), Math.min(limit, maxStackSize));
        ItemStack extracted = stack.split(amount);
        consumer.accept(extracted);
        int count = Math.max(amount - extracted.getCount(), 0);
        stack.grow(extracted.getCount());
        if (stack.getCount() == 0) {
            inventory.setItem(slot, ItemStack.EMPTY);
        } else if (count > 0) {
            inventory.setChanged();
        }
        return count;
    }

    @SuppressWarnings("unused")
    public static void insertIntoInventory(ItemStack stack, Container inventory, Direction side, int limit, boolean simulate, Iterable<Integer> slots) {
        if (stack == null || stack.isEmpty() || limit <= 0) return;
        boolean success = false;
        int remaining = limit;

        for (int slot : slots) {
            int stackSize = stack.getCount();
            if (insertIntoInventorySlot(stack, inventory, side, slot, remaining, simulate)) {
                remaining -= stackSize - stack.getCount();
            }
        }
    }

    @SuppressWarnings("unused")
    public static boolean insertIntoInventory(ItemStack stack, Container inventory, Direction side, int limit, boolean simulate) {
        if (stack == null || stack.isEmpty() || limit <= 0) return false;
        boolean success = false;
        int remaining = limit;

        boolean shouldTryMerge = !stack.getItem().isDamageable(stack) && stack.getMaxStackSize() > 1 && inventory.getMaxStackSize() > 1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            int stackSize = stack.getCount();
            ItemStack existing = inventory.getItem(slot);
            if (!existing.isEmpty() && insertIntoInventorySlot(stack, inventory, side, slot, remaining, simulate)) {
                remaining -= stackSize - stack.getCount();
                success = true;
            }
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            int stackSize = stack.getCount();
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() && insertIntoInventorySlot(stack, inventory, side, slot, remaining, simulate)) {
                remaining -= stackSize - stack.getCount();
                success = true;
            }
        }
        return success;
    }

    public static boolean insertIntoInventory(ItemStack stack, Container inventory, Direction side, int limit) {
        return insertIntoInventory(stack, inventory, side, limit, false);
    }

    public static boolean insertIntoInventory(ItemStack stack, Container inventory, Direction side) {
        return insertIntoInventory(stack, inventory, side, 64, false);
    }

    public static int extractAnyFromInventory(Consumer<ItemStack> consumer, Container inventory, Direction side, int limit) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            int extracted = extractFromInventorySlot(consumer, inventory, side, slot, limit);
            if (extracted > 0) return extracted;
        }
        return 0;
    }

    public static ItemStack extractFromInventory(ItemStack stack, Container inventory, Direction side, boolean simulate, boolean exact) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining.getCount() > 0; slot++) {
            extractFromInventorySlot(stackInInv -> {
                if (stackInInv != null && !stackInInv.isEmpty() && remaining.getItem() == stackInInv.getItem() &&
                        (!exact || haveSameItemType(remaining, stackInInv, true))) {
                    int transferred = Math.min(stackInInv.getCount(), remaining.getCount());
                    remaining.shrink(transferred);
                    if (!simulate) {
                        stackInInv.shrink(transferred);
                    }
                }
            }, inventory, side, slot, remaining.getCount());
        }
        return remaining;
    }

    public static void insertIntoInventoryAt(ItemStack stack, BlockPosition position, Direction side, int limit, boolean simulate) {
        Container inv = inventoryAt(position);
        if (inv != null) {
            insertIntoInventory(stack, inv, side, limit, simulate);
        }
    }

    public static void insertIntoInventoryAt(ItemStack stack, BlockPosition position, Direction side) {
        insertIntoInventoryAt(stack, position, side, 64, false);
    }

    public static int transferBetweenInventories(Container source, Direction sourceSide, Container sink, Direction sinkSide, int limit) {
        int[] transferred = {0};
        extractAnyFromInventory(stack -> {
            int before = stack.getCount();
            insertIntoInventory(stack, sink, sinkSide, limit);
            transferred[0] += before - stack.getCount();
        }, source, sourceSide, limit);
        return transferred[0];
    }

    @FunctionalInterface
    public interface TransferExtractor {
        int extract();
    }

    public static TransferExtractor getTransferBetweenInventoriesAt(BlockPosition sourcePos, Direction sourceSide, BlockPosition sinkPos, Direction sinkSide, int count) {
        Container source = inventoryAt(sourcePos);
        Container sink = inventoryAt(sinkPos);
        if (source == null || sink == null) return null;
        return () -> transferBetweenInventories(source, sourceSide, sink, sinkSide, count);
    }

    public static TransferExtractor getTransferBetweenInventoriesSlotsAt(BlockPosition sourcePos, Direction sourceSide, int sourceSlot, BlockPosition sinkPos, Direction sinkSide, Integer sinkSlot, int count) {
        Container source = inventoryAt(sourcePos);
        Container sink = inventoryAt(sinkPos);
        if (source == null || sink == null) return null;
        return () -> transferBetweenInventoriesSlots(source, sourceSide, sourceSlot, sink, sinkSide, sinkSlot, count);
    }

    public static boolean swapBetweenInventoriesSlots(Container source, Direction sourceSide, int sourceSlot, Container sink, Direction sinkSide, int sinkSlot, boolean safe) {
        ItemStack stackA = source.getItem(sourceSlot).copy();
        ItemStack stackB = sink.getItem(sinkSlot).copy();
        if (stackA.isEmpty() && stackB.isEmpty()) return false;
        if (safe) {
            if (!source.canPlaceItem(sourceSlot, stackB) || !sink.canPlaceItem(sinkSlot, stackA)) return false;
            if (source instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(sourceSlot, stackB, sourceSide))
                return false;
            if (sink instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(sinkSlot, stackA, sinkSide))
                return false;
        }
        source.setItem(sourceSlot, stackB);
        sink.setItem(sinkSlot, stackA);
        source.setChanged();
        sink.setChanged();
        return true;
    }

    private static int transferBetweenInventoriesSlots(Container source, Direction sourceSide, int sourceSlot, Container sink, Direction sinkSide, Integer sinkSlot, int limit) {
        int[] transferred = {0};
        extractFromInventorySlot(stack -> {
            int before = stack.getCount();
            if (sinkSlot != null) {
                insertIntoInventorySlot(stack, sink, sinkSide, sinkSlot, limit);
            } else {
                insertIntoInventory(stack, sink, sinkSide, limit);
            }
            transferred[0] += before - stack.getCount();
        }, source, sourceSide, sourceSlot, limit);
        return transferred[0];
    }

    public static boolean dropSlot(BlockPosition position, Container inventory, int slot, int count, Direction direction) {
        ItemStack stack = inventory.removeItem(slot, count);
        if (!stack.isEmpty()) {
            spawnStackInWorld(position, stack, direction, null);
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean dropSlot(BlockPosition position, Container inventory, int slot, int count) {
        return dropSlot(position, inventory, slot, count, null);
    }

    public static void dropAllSlots(BlockPosition position, Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
                spawnStackInWorld(position, stack, null, null);
            }
        }
    }

    public static void addToPlayerInventory(ItemStack stack, Player player, boolean spawnInWorld) {
        if (stack != null && !stack.isEmpty()) {
            if (player.getInventory().add(stack)) {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
            }
            if (stack.getCount() > 0 && spawnInWorld) {
                player.drop(stack, false);
            }
        }
    }

    public static void addToPlayerInventory(ItemStack stack, Player player) {
        addToPlayerInventory(stack, player, true);
    }

    public static ItemEntity spawnStackInWorld(BlockPosition position, ItemStack stack, Direction direction, java.util.function.Predicate<ItemEntity> validator) {
        if (position.level() != null && stack != null && !stack.isEmpty()) {
            net.minecraft.world.level.Level world = position.level();
            RandomSource rng = world.random;
            int ox = 0, oy = 0, oz = 0;
            if (direction != null) {
                ox = direction.getStepX();
                oy = direction.getStepY();
                oz = direction.getStepZ();
            }
            double tx = 0.1 * (rng.nextDouble() - 0.5) + ox * 0.65;
            double ty = 0.1 * (rng.nextDouble() - 0.5) + oy * 0.75 + (ox + oz) * 0.25;
            double tz = 0.1 * (rng.nextDouble() - 0.5) + oz * 0.65;
            Vec3 dropPos = position.offset(0.5 + tx, 0.5 + ty, 0.5 + tz);
            ItemEntity entity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack.copy());
            entity.setDeltaMovement(
                    0.0125 * (rng.nextDouble() - 0.5) + ox * 0.03,
                    0.0125 * (rng.nextDouble() - 0.5) + oy * 0.08 + (ox + oz) * 0.03,
                    0.0125 * (rng.nextDouble() - 0.5) + oz * 0.03
            );
            entity.lifespan = 6000;
            if (validator == null || validator.test(entity)) {
                world.addFreshEntity(entity);
                return entity;
            }
        }
        return null;
    }

    public interface InventorySource {
        Container inventory();
    }

    public record BlockInventorySource(BlockPosition position, Container inventory) implements InventorySource {
    }

    @SuppressWarnings("unused")
    public record EntityInventorySource(Entity entity, Container inventory) implements InventorySource {
    }

}
