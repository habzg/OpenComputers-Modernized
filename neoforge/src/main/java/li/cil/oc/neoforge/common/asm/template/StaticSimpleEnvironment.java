package li.cil.oc.neoforge.common.asm.template;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SimpleComponent;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.common.asm.template.SimpleComponentImpl;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.neoforge.common.asm.SimpleComponentTickHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("SuspiciousMethodCalls")
public final class StaticSimpleEnvironment {
    private static final Map<BlockEntity, Node> nodes = new HashMap<>();

    @SuppressWarnings("unused")
    private StaticSimpleEnvironment() {
    }

    public static Node node(final SimpleComponentImpl self) {
        return node((BlockEntity) self, self);
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

    public static void validate(final SimpleComponentImpl self) {
        self.validate_OpenComputers();
        SimpleComponentTickHandler.schedule((BlockEntity) self);
    }

    public static void invalidate(final SimpleComponentImpl self) {
        self.invalidate_OpenComputers();
        final Node node = node(self);
        if (node != null) {
            node.remove();
            nodes.remove(self);
        }
    }

    public static void onChunkUnload(final SimpleComponentImpl self) {
        self.onChunkUnload_OpenComputers();
        final Node node = node(self);
        if (node != null) {
            node.remove();
            nodes.remove(self);
        }
    }

    public static void readFromNBT(final SimpleComponentImpl self, CompoundTag nbt) {
        self.readFromNBT_OpenComputers(nbt);
        final Node node = node(self);
        if (node != null) {
            node.load(nbt.getCompound("oc:node"), null);
        }
    }

    public static void writeToNBT(final SimpleComponentImpl self, CompoundTag nbt) {
        self.writeToNBT_OpenComputers(nbt);
        final Node node = node(self);
        if (node != null) {
            final CompoundTag nodeNbt = new CompoundTag();
            node.save(nodeNbt, null);
            nbt.put("oc:node", nodeNbt);
        }
    }

    public static void onServerStopped() {
        nodes.clear();
    }
}
