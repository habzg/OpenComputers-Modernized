package li.cil.oc.core.impl.common.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Charger extends Player {
    public Charger(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Charger charger) {
        super(menuType, containerId, playerInventory, charger);
        addSlot(80, 35, "tablet", li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }
}
