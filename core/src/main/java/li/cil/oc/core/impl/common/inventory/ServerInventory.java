package li.cil.oc.core.impl.common.inventory;

import li.cil.oc.core.Constants;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.common.Tier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ServerInventory extends ItemStackInventory {
    default int tier() {
        return Math.max(0, caseTier(container()));
    }

    private static int caseTier(ItemStack stack) {
        var descriptor = li.cil.oc.api.Items.get(stack);
        if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier1)) return Tier.One;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier2)) return Tier.Two;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier3)) return Tier.Three;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseCreative)) return Tier.Four;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseTier1)) return Tier.One;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseTier2)) return Tier.Two;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseCreative))
            return Tier.Four;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.DroneCaseTier1)) return Tier.One;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.DroneCaseTier2)) return Tier.Two;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.DroneCaseCreative)) return Tier.Four;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier1)) return Tier.One;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier2)) return Tier.Two;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier3)) return Tier.Three;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerCreative)) return Tier.Four;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseTier1)) return Tier.One;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseTier2)) return Tier.Two;
        else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseCreative)) return Tier.Four;
        else return Tier.None;
    }

    @Override
    default int getContainerSize() {
        return InventorySlots.server[tier()].length;
    }

    @Override
    default String inventoryName() {
        return "Server";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    default boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= InventorySlots.server[tier()].length) return false;
        java.util.function.Supplier<Boolean> supplier = () -> {
            Object driver = li.cil.oc.api.API.driver.driverFor(stack, li.cil.oc.api.internal.Server.class);
            if (driver instanceof li.cil.oc.api.driver.Item itemDriver) {
                li.cil.oc.core.common.InventorySlots.InventorySlot provided = InventorySlots.server[tier()][slot];
                return itemDriver.slot(stack).equals(provided.slot()) && itemDriver.tier(stack) <= provided.tier();
            }
            return false;
        };
        return supplier.get();
    }
}
