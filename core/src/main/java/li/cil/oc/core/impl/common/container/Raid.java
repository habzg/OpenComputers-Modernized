package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Raid extends Player {
    public Raid(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Raid raid) {
        super(menuType, containerId, playerInventory, raid);
        addSlot(60, 23, Slot.HDD, Tier.Three);
        addSlot(80, 23, Slot.HDD, Tier.Three);
        addSlot(100, 23, Slot.HDD, Tier.Three);
        addPlayerInventorySlots(8, 84);
    }
}
