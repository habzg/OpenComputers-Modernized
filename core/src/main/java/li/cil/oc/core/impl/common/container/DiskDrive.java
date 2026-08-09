package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.common.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class DiskDrive extends Player {
    public DiskDrive(MenuType<?> menuType, int containerId, Inventory playerInventory, Container drive) {
        super(menuType, containerId, playerInventory, drive);
        addSlot(80, 35, Slot.Floppy, li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }
}
