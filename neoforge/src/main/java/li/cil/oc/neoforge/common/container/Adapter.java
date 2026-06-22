package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.world.entity.player.Inventory;

public class Adapter extends Player {
    public Adapter(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Adapter adapter) {
        super(Menus.ADAPTER.get(), containerId, playerInventory, adapter);
        addSlot(80, 35, Slot.Upgrade, li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }
}
