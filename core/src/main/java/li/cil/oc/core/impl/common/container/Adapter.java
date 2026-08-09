package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.common.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Adapter extends Player {
    public Adapter(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Adapter adapter) {
        super(menuType, containerId, playerInventory, adapter);
        addSlot(80, 35, Slot.Upgrade, li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }
}
