package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Server extends Player {
    public final li.cil.oc.neoforge.server.component.Server server;
    public boolean isRunning = false;
    public boolean isItem = true;
    public net.minecraft.core.BlockPos rackPos = null;
    public int rackSlot = -1;

    public Server(int containerId, Inventory playerInventory, ServerInventory serverInventory, li.cil.oc.neoforge.server.component.Server server, net.minecraft.world.entity.player.Player player) {
        super(Menus.SERVER.get(), containerId, playerInventory, serverInventory, player);
        this.server = server;
        this.playerEntity = player;

        if (server != null) {
            var rack = server.rack();
            if (rack instanceof net.minecraft.world.level.block.entity.BlockEntity te) {
                rackPos = te.getBlockPos();
                rackSlot = server.slot();
            }
        }

        int tier = serverInventory.tier();

        int slotSize = 18;

        for (int i = 0; i <= 1; i++) {
            var invSlot = InventorySlots.server[tier][slots.size()];
            addSlot(76, 7 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        int verticalSlots = Math.min(3, 1 + tier);
        for (int i = 0; i <= verticalSlots; i++) {
            var invSlot = InventorySlots.server[tier][slots.size()];
            addSlot(100, 7 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        for (int i = 0; i <= verticalSlots; i++) {
            var invSlot = InventorySlots.server[tier][slots.size()];
            addSlot(124, 7 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        for (int i = 0; i <= verticalSlots; i++) {
            var invSlot = InventorySlots.server[tier][slots.size()];
            addSlot(148, 7 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        for (int i = 2; i <= verticalSlots; i++) {
            var invSlot = InventorySlots.server[tier][slots.size()];
            addSlot(76, 7 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        {
            var invSlot = InventorySlots.server[tier][slots.size()];
            addSlot(26, 34, invSlot.slot(), invSlot.tier());
        }

        addPlayerInventorySlots(8, 84);
    }

    @Override
    public boolean stillValid(@NotNull net.minecraft.world.entity.player.Player player) {
        if (server != null) return super.stillValid(player);
        if (otherInventory instanceof ServerInventory serverInv) {
            ItemStack container = serverInv.container();
            if (container != null) {
                boolean found = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i) == container) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }
        return player == playerEntity;
    }

    @Override
    public void updateCustomData(CompoundTag nbt) {
        super.updateCustomData(nbt);
        isRunning = nbt.getBoolean("isRunning");
        isItem = nbt.getBoolean("isItem");
        if (nbt.contains("rackPosX")) {
            rackPos = new net.minecraft.core.BlockPos(nbt.getInt("rackPosX"), nbt.getInt("rackPosY"), nbt.getInt("rackPosZ"));
            rackSlot = nbt.getInt("rackSlot");
        }
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        super.detectCustomDataChanges(nbt);
        if (server != null) {
            nbt.putBoolean("isRunning", server.machine().isRunning());
            var rack = server.rack();
            if (rack instanceof net.minecraft.world.level.block.entity.BlockEntity te) {
                var pos = te.getBlockPos();
                nbt.putInt("rackPosX", pos.getX());
                nbt.putInt("rackPosY", pos.getY());
                nbt.putInt("rackPosZ", pos.getZ());
                nbt.putInt("rackSlot", server.slot());
            }
        } else {
            nbt.putBoolean("isItem", true);
        }
    }
}
