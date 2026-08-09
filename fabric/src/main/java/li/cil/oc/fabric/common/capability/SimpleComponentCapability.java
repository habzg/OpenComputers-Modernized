package li.cil.oc.fabric.common.capability;

import java.util.HashSet;
import java.util.Set;
import li.cil.oc.api.capability.SimpleComponentProvider;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SimpleComponent;
import li.cil.oc.fabric.common.EventHandler;
import li.cil.oc.fabric.common.asm.StaticSimpleEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class SimpleComponentCapability implements SimpleComponentProvider {
    private static final Set<BlockEntity> scheduled = new HashSet<>();

    private final BlockEntity blockEntity;
    private final SimpleComponent simpleComponent;

    public SimpleComponentCapability(BlockEntity blockEntity, SimpleComponent simpleComponent) {
        this.blockEntity = blockEntity;
        this.simpleComponent = simpleComponent;
        StaticSimpleEnvironment.node(blockEntity, simpleComponent);
        if (scheduled.add(blockEntity)) {
            EventHandler.scheduleServer(blockEntity);
        }
    }

    @Override
    public Node node() {
        return StaticSimpleEnvironment.node(blockEntity, simpleComponent);
    }

    @Override
    public String getComponentName() {
        return simpleComponent.getComponentName();
    }

    @SuppressWarnings("unused")
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag nbt = new CompoundTag();
        Node node = StaticSimpleEnvironment.node(blockEntity, simpleComponent);
        if (node != null) {
            CompoundTag nodeNbt = new CompoundTag();
            node.save(nodeNbt, provider);
            nbt.put("oc:node", nodeNbt);
        }
        return nbt;
    }

    @SuppressWarnings("unused")
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag nbt) {
        if (nbt.contains("oc:node")) {
            Node node = StaticSimpleEnvironment.node(blockEntity, simpleComponent);
            if (node != null) {
                try {
                    node.load(nbt.getCompound("oc:node"), provider);
                } catch (Exception e) {
                    li.cil.oc.fabric.OpenComputers.log().warn("Failed to load SimpleComponent node", e);
                }
            }
        }
    }
}
