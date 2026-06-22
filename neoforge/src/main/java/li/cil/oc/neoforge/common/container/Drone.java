package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class Drone extends Player {
    public final li.cil.oc.core.impl.common.entity.Drone drone;

    public Drone(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.entity.Drone drone) {
        super(Menus.DRONE.get(), containerId, playerInventory, drone.mainInventory);
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
            return getSlotIndex() >= 0 && getSlotIndex() < drone.mainInventory.getContainerSize();
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
