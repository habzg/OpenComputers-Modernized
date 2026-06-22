package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class Case extends li.cil.oc.neoforge.common.container.Player {
    private final li.cil.oc.core.impl.common.tileentity.Case computer;

    public Case(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Case computer) {
        this(menuType, containerId, playerInventory, computer, computer.tier());
    }

    public Case(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Case computer, int tier) {
        super(menuType, containerId, playerInventory, computer);
        this.computer = computer;

        int slotSize = 18;
        int limit = tier >= Tier.Three ? 2 : 1;
        for (int i = 0; i <= limit; i++) {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(98, 16 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        limit = tier == Tier.One ? 0 : 1;
        for (int i = 0; i <= limit; i++) {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(120, 16 + (i + 1) * slotSize, invSlot.slot(), invSlot.tier());
        }

        for (int i = 0; i <= limit; i++) {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(142, 16 + i * slotSize, invSlot.slot(), invSlot.tier());
        }

        if (tier >= Tier.Three) {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(142, 16 + 2 * slotSize, invSlot.slot(), invSlot.tier());
        }

        {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(120, 16, invSlot.slot(), invSlot.tier());
        }

        if (tier == Tier.One) {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(120, 16 + 2 * slotSize, invSlot.slot(), invSlot.tier());
        }

        {
            var invSlot = InventorySlots.computer[tier][slots.size()];
            addSlot(48, 34, invSlot.slot(), invSlot.tier());
        }

        addPlayerInventorySlots(8, 84);
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.@NotNull Player player) {
        return super.stillValid(player) && computer.isUseableByPlayer(player);
    }
}
