package li.cil.oc.neoforge.common.container;

import li.cil.oc.neoforge.common.init.Menus;
import li.cil.oc.neoforge.common.inventory.DatabaseInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Database extends li.cil.oc.neoforge.common.container.Player {
    private final int databaseSize;

    public Database(int containerId, Inventory playerInventory, DatabaseInventory databaseInventory) {
        super(Menus.DATABASE.get(), containerId, playerInventory, databaseInventory);

        databaseSize = databaseInventory.getContainerSize();
        int rows = (int) Math.ceil(Math.sqrt(databaseSize));
        int offset = 8 + new int[]{3, 2, 0}[databaseInventory.tier()] * slotSize;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < rows; col++) {
                addSlot(offset + col * slotSize, offset + row * slotSize);
            }
        }

        addPlayerInventorySlots(8, 174);
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.@NotNull Player player) {
        return player == playerInventory.player;
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, net.minecraft.world.entity.player.@NotNull Player player) {
        if (slotId >= 0 && slotId < databaseSize) {
            Slot ghostSlot = getSlot(slotId);
            if (clickType == ClickType.PICKUP) {
                ItemStack carried = getCarried();
                ItemStack toPlace = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
                ghostSlot.set(toPlace);
                if (!player.level().isClientSide) broadcastChanges();
                return;
            } else if (clickType == ClickType.QUICK_MOVE) {
                ghostSlot.set(ItemStack.EMPTY);
                if (!player.level().isClientSide) broadcastChanges();
                return;
            } else if (clickType == ClickType.SWAP && button >= 0 && button < 9) {
                ItemStack hotbarStack = playerInventory.getItem(button);
                ItemStack toPlace = hotbarStack.isEmpty() ? ItemStack.EMPTY : hotbarStack.copyWithCount(1);
                ghostSlot.set(toPlace);
                if (!player.level().isClientSide) broadcastChanges();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    protected void tryTransferStackInSlot(Slot from, boolean intoPlayerInventory) {
        if (intoPlayerInventory) {
            from.setChanged();
            return;
        }
        ItemStack fromStack = from.getItem().copy();
        if (fromStack.isEmpty()) return;
        fromStack.setCount(1);
        for (Slot intoSlot : slots) {
            if (intoSlot.container != from.container) {
                if (!intoSlot.hasItem() && intoSlot.mayPlace(fromStack)) {
                    if (intoSlot.getMaxStackSize() > 0) {
                        intoSlot.set(fromStack);
                        return;
                    }
                }
            }
        }
    }
}
