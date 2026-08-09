package li.cil.oc.neoforge.common.capability;

import java.util.HashSet;
import java.util.Set;
import li.cil.oc.api.capability.SimpleComponentProvider;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SimpleComponent;
import li.cil.oc.neoforge.common.asm.SimpleComponentTickHandler;
import li.cil.oc.neoforge.common.asm.template.StaticSimpleEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

public class SimpleComponentCapability implements SimpleComponentProvider, INBTSerializable<CompoundTag> {
    private static final Set<BlockEntity> scheduled = new HashSet<>();

    private final BlockEntity blockEntity;
    private final SimpleComponent simpleComponent;

    @SuppressWarnings("unused")
    public SimpleComponentCapability(BlockEntity blockEntity, SimpleComponent simpleComponent) {
        this.blockEntity = blockEntity;
        this.simpleComponent = simpleComponent;
        StaticSimpleEnvironment.node(blockEntity, simpleComponent);
        if (scheduled.add(blockEntity)) {
            SimpleComponentTickHandler.schedule(blockEntity);
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

    @Override
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

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag nbt) {
        if (nbt.contains("oc:node")) {
            Node node = StaticSimpleEnvironment.node(blockEntity, simpleComponent);
            if (node != null) {
                try {
                    node.load(nbt.getCompound("oc:node"), provider);
                } catch (Exception e) {
                    li.cil.oc.neoforge.OpenComputers.log().warn("Failed to load SimpleComponent node", e);
                }
            }
        }
    }
}
