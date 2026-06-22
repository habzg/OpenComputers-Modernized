package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.world.entity.player.Inventory;

public class DiskDrive extends Player {
    public DiskDrive(int containerId, Inventory playerInventory, net.minecraft.world.Container drive) {
        super(Menus.DISK_DRIVE.get(), containerId, playerInventory, drive);
        addSlot(80, 35, Slot.Floppy, li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }
}
