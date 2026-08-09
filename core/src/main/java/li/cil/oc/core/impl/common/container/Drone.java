package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class Drone extends Player {
    public final li.cil.oc.core.impl.common.entity.Drone drone;

    public Drone(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.entity.Drone drone) {
        super(menuType, containerId, playerInventory, drone.mainInventory);
        this.drone = drone;

        for (int i = 0; i <= 1; i++) {
            int slotSize = 18;
            int y = 8 + i * slotSize;
            for (int j = 0; j <= 3; j++) {
                int x = 98 + j * slotSize;
                addSlot(new InventorySlot(this, otherInventory, slots.size(), x, y));
            }
        }

        addPlayerInventorySlots(8, 66);
    }

    class InventorySlot extends StaticComponentSlot {
        public InventorySlot(Player container, Container inventory, int index, int x, int y) {
            super(container, inventory, index, x, y, Slot.Any, Tier.Any);
        }

        public boolean isValid() {
            return getContainerSlot() >= 0 && getContainerSlot() < drone.mainInventory.getContainerSize();
        }

        @Override
        public boolean isActive() {
            return isValid() && super.isActive();
        }

        @Override
        public net.minecraft.world.item.@NotNull ItemStack getItem() {
            if (isValid()) return super.getItem();
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }
}
