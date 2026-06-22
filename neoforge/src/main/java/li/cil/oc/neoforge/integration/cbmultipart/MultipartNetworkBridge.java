package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.multipart.api.NormalOcclusionTest;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.block.TileMultipart;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.util.Color;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MultipartNetworkBridge {
    private MultipartNetworkBridge() {
    }

    public static void install() {
        li.cil.oc.core.impl.server.network.Network.multipartNodeHandler = MultipartNetworkBridge::getNode;
        li.cil.oc.core.impl.server.network.Network.multipartColorHandler = MultipartNetworkBridge::getColor;
        li.cil.oc.core.impl.server.network.Network.multipartCanConnectHandler = MultipartNetworkBridge::canConnectFromSide;
    }

    private static Node getNode(BlockEntity tileEntity) {
        if (!(tileEntity instanceof TileMultipart tile)) return null;
        for (MultiPart part : tile.getPartList()) {
            if (part instanceof li.cil.oc.api.network.Environment env) {
                return env.node();
            }
        }
        return null;
    }

    private static int getColor(BlockEntity tileEntity) {
        if (!(tileEntity instanceof TileMultipart tile)) return Color.LightGray;
        for (MultiPart part : tile.getPartList()) {
            if (part instanceof CablePart cable) {
                return cable.getColor();
            }
        }
        return Color.LightGray;
    }

    public static boolean canConnectFromSide(BlockEntity tileEntity, Direction side) {
        if (!(tileEntity instanceof TileMultipart tile)) return true;
        VoxelShape armShape = li.cil.oc.neoforge.common.block.Cable.armFor(side);
        var armPart = NormalOcclusionTest.of(armShape);
        for (MultiPart part : tile.getPartList()) {
            if (part instanceof CablePart) continue;
            if (!NormalOcclusionTest.test(armPart, part)) {
                return false;
            }
        }
        return true;
    }
}
