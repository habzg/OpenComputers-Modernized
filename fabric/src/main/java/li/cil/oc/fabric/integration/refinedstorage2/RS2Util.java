package li.cil.oc.fabric.integration.refinedstorage2;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockItem;
import com.refinedmods.refinedstorage.common.exporter.AbstractExporterBlockEntity;
import com.refinedmods.refinedstorage.common.iface.InterfaceBlockEntity;
import com.refinedmods.refinedstorage.common.importer.AbstractImporterBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RS2Util {
    private RS2Util() {
    }

    public static Class<?> controllerClass() {
        return com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity.class;
    }

    public static Class<?> importerClass() {
        return AbstractImporterBlockEntity.class;
    }

    public static Class<?> exporterClass() {
        return AbstractExporterBlockEntity.class;
    }

    public static Class<?> interfaceClass() {
        return InterfaceBlockEntity.class;
    }

    public static boolean isController(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ControllerBlockItem;
    }

    public static boolean isImporter(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Blocks.INSTANCE.getImporter().getDefault().asItem());
    }

    public static boolean isExporter(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Blocks.INSTANCE.getExporter().getDefault().asItem());
    }

    public static boolean isInterface(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Blocks.INSTANCE.getInterface().asItem());
    }

    public static Network networkOf(BlockEntity tile) {
        if (tile instanceof AbstractNetworkNodeContainerBlockEntity<?> container) {
            for (var nodeContainer : container.getContainerProvider().getContainers()) {
                var node = nodeContainer.getNode();
                var network = node.getNetwork();
                if (network != null) {
                    return network;
                }
            }
        }
        return null;
    }
}
