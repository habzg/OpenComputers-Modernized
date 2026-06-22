package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.world.entity.player.Inventory;

public class Raid extends Player {
    public Raid(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Raid raid) {
        super(Menus.RAID.get(), containerId, playerInventory, raid);
        addSlot(60, 23, Slot.HDD, Tier.Three);
        addSlot(80, 23, Slot.HDD, Tier.Three);
        addSlot(100, 23, Slot.HDD, Tier.Three);
        addPlayerInventorySlots(8, 84);
    }
}
