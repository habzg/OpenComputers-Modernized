package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.InventorySlots.InventorySlot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class Player extends AbstractContainerMenu {
    public final Inventory playerInventory;
    public final Container otherInventory;
    protected final int playerInventorySizeX = 9;
    protected final int playerInventorySizeY = 4;
    protected final int slotSize = 18;
    protected final SynchronizedData synchronizedData = new SynchronizedData();
    public net.minecraft.world.entity.player.Player playerEntity;

    protected Player(MenuType<?> menuType, int containerId, Inventory playerInventory, Container otherInventory) {
        super(menuType, containerId);
        this.playerInventory = playerInventory;
        this.otherInventory = otherInventory;
    }

    protected Player(MenuType<?> menuType, int containerId, Inventory playerInventory, Container otherInventory, net.minecraft.world.entity.player.Player player) {
        super(menuType, containerId);
        this.playerInventory = playerInventory;
        this.otherInventory = otherInventory;
        this.playerEntity = player;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.@NotNull Player player) {
        return otherInventory.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(net.minecraft.world.entity.player.@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            tryTransferStackInSlot(slot, slot.container == otherInventory);
            if (SideTracker.isServer()) {
                broadcastChanges();
            }
        }
        return ItemStack.EMPTY;
    }

    protected boolean tryMoveAllSlotToSlot(Slot from, Slot to) {
        if (to == null) return false;
        if (from == null || !from.hasItem() || from.getItem().isEmpty()) return true;

        if (to.container == from.container) return false;

        ItemStack fromStack = from.getItem();
        ItemStack toStack = to.hasItem() ? to.getItem() : null;
        int toStackSize = toStack != null ? toStack.getCount() : 0;

        int maxStackSize = Math.min(fromStack.getMaxStackSize(), to.getMaxStackSize());
        int itemsMoved = Math.min(maxStackSize - toStackSize, fromStack.getCount());

        if (toStack != null) {
            if (toStackSize < maxStackSize &&
                    ItemStack.isSameItemSameComponents(fromStack, toStack) &&
                    itemsMoved > 0) {
                toStack.grow(from.remove(itemsMoved).getCount());
            } else return false;
        } else if (to.mayPlace(fromStack)) {
            to.set(from.remove(itemsMoved));
            if (maxStackSize == 0) return true;
        } else return false;

        to.setChanged();
        from.setChanged();
        return false;
    }

    protected List<Integer> fillOrder(boolean backFill) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) indices.add(i);
        if (backFill) java.util.Collections.reverse(indices);
        indices.sort(java.util.Comparator.comparingInt((Integer i) -> {
            Slot s = slots.get(i);
            if (s.hasItem()) return -1;
            if (s instanceof ComponentSlot cs) return cs.tier();
            return 99;
        }));
        return indices;
    }

    protected void tryTransferStackInSlot(Slot from, boolean intoPlayerInventory) {
        for (int i : fillOrder(intoPlayerInventory)) {
            Slot slot = slots.get(i);
            if (tryMoveAllSlotToSlot(from, slot)) return;
        }
    }

    protected void addSlot(int x, int y) {
        addSlot(x, y, li.cil.oc.core.common.Slot.Any, Tier.Any);
    }

    protected void addSlot(int x, int y, String slotType, int tier) {
        int index = slots.size();
        addSlot(new StaticComponentSlot(this, otherInventory, index, x, y, slotType, tier));
    }

    protected void addSlot(int x, int y, Function<DynamicComponentSlot, InventorySlot> info) {
        int index = slots.size();
        addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, info, () -> Tier.One));
    }

    protected void addPlayerInventorySlots(int left, int top) {
        for (int slotY = 1; slotY < playerInventorySizeY; slotY++) {
            for (int slotX = 0; slotX < playerInventorySizeX; slotX++) {
                int index = slotX + slotY * playerInventorySizeX;
                int x = left + slotX * slotSize;
                int y = top + (slotY - 1) * slotSize;
                addSlot(new Slot(playerInventory, index, x, y));
            }
        }

        int quickBarSpacing = 4;
        for (int index = 0; index < playerInventorySizeX; index++) {
            int x = left + index * slotSize;
            int y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing;
            addSlot(new Slot(playerInventory, index, x, y));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (SideTracker.isServer()) {
            CompoundTag nbt = new CompoundTag();
            detectCustomDataChanges(nbt);
            if (playerEntity instanceof ServerPlayer serverPlayer) {
                PacketSender.sendContainerUpdate(this, nbt, serverPlayer);
            }
        }
    }

    protected void detectCustomDataChanges(CompoundTag nbt) {
        CompoundTag delta = synchronizedData.getDelta();
        if (delta != null && !delta.isEmpty()) {
            nbt.put("delta", delta);
        }
    }

    public void updateCustomData(CompoundTag nbt) {
        if (nbt.contains("delta")) {
            CompoundTag delta = nbt.getCompound("delta");
            for (String key : delta.getAllKeys()) {
                Tag value = delta.get(key);
                if (value != null) {
                    synchronizedData.put(key, value);
                }
            }
        }
    }

    protected static class SynchronizedData extends CompoundTag {
        private CompoundTag delta = new CompoundTag();

        public CompoundTag getDelta() {
            synchronized (this) {
                if (delta.isEmpty()) return null;
                CompoundTag result = delta;
                delta = new CompoundTag();
                return result;
            }
        }

        @Override
        public Tag put(@NotNull String key, Tag value) {
            synchronized (this) {
                if (!value.equals(get(key))) delta.put(key, value);
                return super.put(key, value);
            }
        }

        @Override
        public void putByte(@NotNull String key, byte value) {
            synchronized (this) {
                if (value != getByte(key)) delta.putByte(key, value);
                super.putByte(key, value);
            }
        }

        @Override
        public void putShort(@NotNull String key, short value) {
            synchronized (this) {
                if (value != getShort(key)) delta.putShort(key, value);
                super.putShort(key, value);
            }
        }

        @Override
        public void putInt(@NotNull String key, int value) {
            synchronized (this) {
                if (value != getInt(key)) delta.putInt(key, value);
                super.putInt(key, value);
            }
        }

        @Override
        public void putLong(@NotNull String key, long value) {
            synchronized (this) {
                if (value != getLong(key)) delta.putLong(key, value);
                super.putLong(key, value);
            }
        }

        @Override
        public void putFloat(@NotNull String key, float value) {
            synchronized (this) {
                if (value != getFloat(key)) delta.putFloat(key, value);
                super.putFloat(key, value);
            }
        }

        @Override
        public void putDouble(@NotNull String key, double value) {
            synchronized (this) {
                if (value != getDouble(key)) delta.putDouble(key, value);
                super.putDouble(key, value);
            }
        }

        @Override
        public void putString(@NotNull String key, String value) {
            synchronized (this) {
                if (!value.equals(getString(key))) delta.putString(key, value);
                super.putString(key, value);
            }
        }

        @Override
        public void putByteArray(@NotNull String key, byte @NotNull [] value) {
            synchronized (this) {
                if (!java.util.Arrays.equals(value, getByteArray(key))) delta.putByteArray(key, value);
                super.putByteArray(key, value);
            }
        }

        @Override
        public void putIntArray(@NotNull String key, int @NotNull [] value) {
            synchronized (this) {
                if (!java.util.Arrays.equals(value, getIntArray(key))) delta.putIntArray(key, value);
                super.putIntArray(key, value);
            }
        }

        @Override
        public void putBoolean(@NotNull String key, boolean value) {
            synchronized (this) {
                if (value != getBoolean(key)) delta.putBoolean(key, value);
                super.putBoolean(key, value);
            }
        }
    }
}
