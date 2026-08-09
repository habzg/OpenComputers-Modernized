package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.impl.common.item.TabletWrapper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Tablet extends Player {
    public Tablet(MenuType<?> menuType, int containerId, Inventory playerInventory, TabletWrapper tablet) {
        super(menuType, containerId, playerInventory, tablet);
        addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize() - 1, 80, 35,
                tablet.containerSlotType(), tablet.containerSlotTier()));
        addPlayerInventorySlots(8, 84);
    }
}
