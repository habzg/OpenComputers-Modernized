package li.cil.oc.fabric.integration.vanilla;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.core.impl.util.BlockPosition;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public final class DriverInventory extends DriverSidedBlockEntity {
    @Override
    public boolean isGeneric() {
        return true;
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return Container.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level level, final BlockPos pos, final Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return new Environment((Container) blockEntity, level);
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<Container> {
        private final Player fakePlayer;
        private final BlockPosition position;

        public Environment(final Container container, final Level world) {
          super(container, "inventory");
            fakePlayer =
                    FakePlayer.get((ServerLevel) world, OCSettings.get().fakePlayerProfile);
            var blockPos = ((BlockEntity) container).getBlockPos();
            position = BlockPosition.apply(blockPos.getX(), blockPos.getY(), blockPos.getZ(), world);
        }

        @Callback(doc = "function():string -- Get the name of this inventory.")
        public Object[] getInventoryName(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            if (this.getBlockEntity() instanceof Nameable nameable) {
                return new Object[]{nameable.getDisplayName().getString()};
            }
            return new Object[]{null};
        }

        @Callback(doc = "function():number -- Get the number of slots in this inventory.")
        public Object[] getInventorySize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            return new Object[]{this.getBlockEntity().getContainerSize()};
        }

        @Callback(doc = "function(slot:number):number -- Get the stack size of the item stack in the specified slot.")
        public Object[] getSlotStackSize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slot = checkSlot(args, 0);
            final ItemStack stack = this.getBlockEntity().getItem(slot);
            return new Object[]{stack.getCount()};
        }

        @Callback(
                doc =
                        "function(slot:number):number -- Get the maximum stack size of the item stack in the specified slot.")
        public Object[] getSlotMaxStackSize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slot = checkSlot(args, 0);
            final ItemStack stack = this.getBlockEntity().getItem(slot);
            if (!stack.isEmpty()) {
                return new Object[]{Math.min(this.getBlockEntity().getMaxStackSize(), stack.getMaxStackSize())};
            } else {
                return new Object[]{this.getBlockEntity().getMaxStackSize()};
            }
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
            final ItemStack stackA = this.getBlockEntity().getItem(slotA);
            final ItemStack stackB = this.getBlockEntity().getItem(slotB);
            if (stackA.isEmpty() && stackB.isEmpty()) {
                return new Object[]{true};
            } else if (!stackA.isEmpty() && !stackB.isEmpty()) {
                return new Object[]{itemEquals(stackA, stackB)};
            } else {
                return new Object[]{false};
            }
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
                    this.getBlockEntity().getMaxStackSize());
            if (slotA == slotB || count == 0) {
                return new Object[]{true};
            }
            final ItemStack stackA = this.getBlockEntity().getItem(slotA);
            final ItemStack stackB = this.getBlockEntity().getItem(slotB);
            if (stackA.isEmpty()) {
                return new Object[]{false};
            } else if (stackB.isEmpty()) {
                final ItemStack moved = stackA.copyWithCount(Math.min(count, stackA.getCount()));
                stackA.shrink(moved.getCount());
                this.getBlockEntity().setItem(slotB, moved);
                if (stackA.isEmpty()) {
                    this.getBlockEntity().setItem(slotA, ItemStack.EMPTY);
                }
                this.getBlockEntity().setChanged();
                return new Object[]{true};
            } else if (itemEquals(stackA, stackB)) {
                final int space =
                        Math.min(this.getBlockEntity().getMaxStackSize(), stackB.getMaxStackSize()) - stackB.getCount();
                final int amount = Math.min(count, Math.min(space, stackA.getCount()));
                if (amount > 0) {
                    stackA.shrink(amount);
                    stackB.grow(amount);
                    if (stackA.isEmpty()) {
                        this.getBlockEntity().setItem(slotA, ItemStack.EMPTY);
                    }
                    this.getBlockEntity().setChanged();
                    return new Object[]{true};
                }
            } else if (count >= stackA.getCount()) {
                this.getBlockEntity().setItem(slotB, stackA);
                this.getBlockEntity().setItem(slotA, stackB);
                return new Object[]{true};
            }
            return new Object[]{false};
        }

        @Callback(doc = "function(slot:number):table -- Get a description of the item stack in the specified slot.")
        public Object[] getStackInSlot(final Context context, final Arguments args) {
            if (OCSettings.get().allowItemStackInspection) {
                if (notPermitted()) return new Object[]{null, "permission denied"};
                return new Object[]{this.getBlockEntity().getItem(checkSlot(args, 0))};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        @Callback(doc = "function():table -- Get a list of descriptions for all item stacks in this inventory.")
        public Object[] getAllStacks(final Context context, final Arguments args) {
            if (OCSettings.get().allowItemStackInspection) {
                if (notPermitted()) return new Object[]{null, "permission denied"};
                ItemStack[] allStacks = new ItemStack[this.getBlockEntity().getContainerSize()];
                for (int i = 0; i < this.getBlockEntity().getContainerSize(); i++) {
                    allStacks[i] = this.getBlockEntity().getItem(i);
                }
                return new Object[]{allStacks};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        private int checkSlot(final Arguments args, final int number) {
            final int slot = args.checkInteger(number) - 1;
            if (slot < 0 || slot >= this.getBlockEntity().getContainerSize()) {
                throw new IllegalArgumentException("slot index out of bounds");
            }
            return slot;
        }

        private boolean itemEquals(final ItemStack stackA, final ItemStack stackB) {
            return ItemStack.isSameItemSameComponents(stackA, stackB);
        }

        private boolean notPermitted() {
            synchronized (fakePlayer) {
                fakePlayer.setPos(position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
                return !li.cil.oc.core.impl.server.component.traits.WorldAction.mayInteract(fakePlayer.level(), position, Direction.DOWN)
                        || !this.getBlockEntity().stillValid(fakePlayer);
            }
        }
    }
}
