package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.impl.common.item.TabletWrapper;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.world.entity.player.Inventory;

public class Tablet extends Player {
    public Tablet(int containerId, Inventory playerInventory, TabletWrapper tablet) {
        super(Menus.TABLET.get(), containerId, playerInventory, tablet);
        addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize() - 1, 80, 35,
                tablet.containerSlotType(), tablet.containerSlotTier()));
        addPlayerInventorySlots(8, 84);
    }
}
