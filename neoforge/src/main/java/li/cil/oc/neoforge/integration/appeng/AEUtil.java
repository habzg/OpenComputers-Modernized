package li.cil.oc.neoforge.integration.appeng;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.storage.MEStorage;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import net.minecraft.world.item.ItemStack;

public final class AEUtil {
    private AEUtil() {
    }

    public static Class<?> controllerClass() {
        try {
            return Class.forName("appeng.blockentity.networking.ControllerBlockEntity");
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    public static Class<?> interfaceClass() {
        return InterfaceBlockEntity.class;
    }

    public static boolean isController(ItemStack stack) {
        return stack != null && !stack.isEmpty() && AEBlocks.CONTROLLER.is(stack);
    }

    public static boolean isExportBus(ItemStack stack) {
        return stack != null && !stack.isEmpty() && AEParts.EXPORT_BUS.is(stack);
    }

    public static boolean isImportBus(ItemStack stack) {
        return stack != null && !stack.isEmpty() && AEParts.IMPORT_BUS.is(stack);
    }

    public static boolean isBlockInterface(ItemStack stack) {
        return stack != null && !stack.isEmpty() && AEBlocks.INTERFACE.is(stack);
    }

    public static boolean isPartInterface(ItemStack stack) {
        return stack != null && !stack.isEmpty() && AEParts.INTERFACE.is(stack);
    }

    public static MEStorage getGridStorage(IGrid grid) {
        return grid.getStorageService().getInventory();
    }

    public static ICraftingService getGridCrafting(IGrid grid) {
        return grid.getCraftingService();
    }

    public static IEnergyService getGridEnergy(IGrid grid) {
        return grid.getEnergyService();
    }
}
