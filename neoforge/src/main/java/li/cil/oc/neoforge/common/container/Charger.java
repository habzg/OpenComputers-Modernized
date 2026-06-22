package li.cil.oc.neoforge.common.container;

import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.world.entity.player.Inventory;

public class Charger extends Player {
    public Charger(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Charger charger) {
        super(Menus.CHARGER.get(), containerId, playerInventory, charger);
        addSlot(80, 35, "tablet", li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }
}
