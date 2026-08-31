package li.cil.oc.neoforge.common.asm.template;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SimpleComponent;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class StaticSimpleEnvironment {
    private static final Map<BlockEntity, Node> nodes = new HashMap<>();

    @SuppressWarnings("unused")
    private StaticSimpleEnvironment() {
    }

    public static Node node(final BlockEntity blockEntity, final SimpleComponent simpleComponent) {
        if (SideTracker.isClient()) {
            return null;
        }
        final String name = simpleComponent.getComponentName();
        if (Strings.isNullOrEmpty(name)) {
            final Node node = nodes.remove(blockEntity);
            if (node != null) {
                node.remove();
            }
        } else if (!nodes.containsKey(blockEntity)) {
            nodes.put(
                    blockEntity,
                    Network.newNode(blockEntity instanceof Environment env ? env : null, Visibility.Network)
                            .withComponent(name)
                            .create());
        }
        return nodes.get(blockEntity);
    }

    public static void onServerStopped() {
        nodes.clear();
    }
}
