package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@SuppressWarnings("unused")
public final class DriverInventory extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return Container.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level level, final int x, final int y, final int z, final Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
        return new Environment((Container) blockEntity, level);
    }

    public static final class Environment extends ManagedTileEntityEnvironment<Container> {
        private final Player fakePlayer;
        private final BlockPosition position;

        public Environment(final Container container, final Level world) {
            super(container, "inventory");
            fakePlayer =
                    FakePlayerFactory.get((ServerLevel) world, Settings.get().fakePlayerProfile);
            var blockPos = ((BlockEntity) container).getBlockPos();
            position = BlockPosition.apply(blockPos.getX(), blockPos.getY(), blockPos.getZ(), world);
        }

        @Callback(doc = "function():string -- Get the name of this inventory.")
        public Object[] getInventoryName(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            return new Object[]{getTileEntity()};
        }

        @Callback(doc = "function():number -- Get the number of slots in this inventory.")
        public Object[] getInventorySize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            return new Object[]{getTileEntity().getContainerSize()};
        }

        @Callback(doc = "function(slot:number):number -- Get the stack size of the item stack in the specified slot.")
        public Object[] getSlotStackSize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slot = checkSlot(args, 0);
            final ItemStack stack = getTileEntity().getItem(slot);
            return new Object[]{stack.getCount()};
        }

        @Callback(
                doc =
                        "function(slot:number):number -- Get the maximum stack size of the item stack in the specified slot.")
        public Object[] getSlotMaxStackSize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slot = checkSlot(args, 0);
            final ItemStack stack = getTileEntity().getItem(slot);
            return new Object[]{Math.min(getTileEntity().getMaxStackSize(), stack.getMaxStackSize())};
        }

        @Callback(
                doc =
                        "function(slotA:number, slotB:number):boolean -- Compare the two item stacks in the specified slots for equality.")
        public Object[] compareStacks(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slotA = checkSlot(args, 0);
            final int slotB = checkSlot(args, 1);
            if (slotA == slotB) {
                return new Object[]{true};
            }
            final ItemStack stackA = getTileEntity().getItem(slotA);
            final ItemStack stackB = getTileEntity().getItem(slotB);
            return new Object[]{itemEquals(stackA, stackB)};
        }

        @Callback(
                doc =
                        "function(slotA:number, slotB:number[, count:number=math.huge]):boolean -- Move up to the specified number of items from the first specified slot to the second.")
        public Object[] transferStack(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slotA = checkSlot(args, 0);
            final int slotB = checkSlot(args, 1);
            final int count = Math.clamp(
                    args.count() > 2 && args.checkAny(2) != null ? args.checkInteger(2) : 64,
                    0,
                    getTileEntity().getMaxStackSize());
            if (slotA == slotB || count == 0) {
                return new Object[]{true};
            }
            final ItemStack stackA = getTileEntity().getItem(slotA);
            final ItemStack stackB = getTileEntity().getItem(slotB);
            if (itemEquals(stackA, stackB)) {
                final int space =
                        Math.min(getTileEntity().getMaxStackSize(), stackB.getMaxStackSize()) - stackB.getCount();
                final int amount = Math.min(count, Math.min(space, stackA.getCount()));
                if (amount > 0) {
                    stackA.shrink(amount);
                    stackB.grow(amount);
                    if (stackA.getCount() == 0) {
                        getTileEntity().setItem(slotA, ItemStack.EMPTY);
                    }
                    getTileEntity().setChanged();
                    return new Object[]{true};
                }
            } else if (count >= stackA.getCount()) {
                getTileEntity().setItem(slotB, stackA);
                getTileEntity().setItem(slotA, stackB);
                return new Object[]{true};
            }
            return new Object[]{false};
        }

        @Callback(doc = "function(slot:number):table -- Get a description of the item stack in the specified slot.")
        public Object[] getStackInSlot(final Context context, final Arguments args) {
            if (Settings.get().allowItemStackInspection) {
                if (notPermitted()) return new Object[]{null, "permission denied"};
                return new Object[]{getTileEntity().getItem(checkSlot(args, 0))};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        @Callback(doc = "function():table -- Get a list of descriptions for all item stacks in this inventory.")
        public Object[] getAllStacks(final Context context, final Arguments args) {
            if (Settings.get().allowItemStackInspection) {
                if (notPermitted()) return new Object[]{null, "permission denied"};
                ItemStack[] allStacks = new ItemStack[getTileEntity().getContainerSize()];
                for (int i = 0; i < getTileEntity().getContainerSize(); i++) {
                    allStacks[i] = getTileEntity().getItem(i);
                }
                return new Object[]{allStacks};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        private int checkSlot(final Arguments args, final int number) {
            final int slot = args.checkInteger(number) - 1;
            if (slot < 0 || slot >= getTileEntity().getContainerSize()) {
                throw new IllegalArgumentException("slot index out of bounds");
            }
            return slot;
        }

        private boolean itemEquals(final ItemStack stackA, final ItemStack stackB) {
            return ItemStack.isSameItem(stackA, stackB);
        }

        private boolean notPermitted() {
            synchronized (fakePlayer) {
                fakePlayer.setPos(position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
                return !getTileEntity().stillValid(fakePlayer);
            }
        }
    }
}
