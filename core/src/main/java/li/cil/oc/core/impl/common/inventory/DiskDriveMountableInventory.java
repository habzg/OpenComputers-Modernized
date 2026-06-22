package li.cil.oc.core.impl.common.inventory;

import li.cil.oc.api.Driver;
import li.cil.oc.core.common.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface DiskDriveMountableInventory extends ItemStackInventory {
    @SuppressWarnings({"SameReturnValue", "unused"})
    default int tier() {
        return 1;
    }

    @Override
    default int getContainerSize() {
        return 1;
    }

    @Override
    default String inventoryName() {
        return "diskdrive";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            var driver = Driver.driverFor(stack, li.cil.oc.core.impl.common.tileentity.DiskDrive.class);
            return driver != null && Slot.Floppy.equals(driver.slot(stack));
        }
        return false;
    }
}
