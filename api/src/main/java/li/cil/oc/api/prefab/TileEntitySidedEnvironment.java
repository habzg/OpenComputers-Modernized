package li.cil.oc.api.prefab;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@SuppressWarnings("unused")
public abstract class TileEntitySidedEnvironment extends BlockEntity implements Environment, SidedEnvironment {
    protected final Node[] nodes = new Node[6];

    protected boolean addedToNetwork = false;

    protected TileEntitySidedEnvironment(BlockEntityType<?> type, BlockPos pos, BlockState state, final Node... nodes) {
        super(type, pos, state);
        System.arraycopy(nodes, 0, this.nodes, 0, Math.min(nodes.length, this.nodes.length));
    }

    @Override
    public Node node() {
        return null;
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : nodes[side.ordinal()];
    }

    public void updateEntity() {
        if (!addedToNetwork) {
            addedToNetwork = true;
            Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (Node node : nodes) {
            if (node != null) node.remove();
        }
    }

    @Override
    public void loadAdditional(final @NotNull CompoundTag nbt, final HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(nbt, registries);
        int index = 0;
        for (Node node : nodes) {
            if (node != null && Objects.equals(node.host(), this)) {
                node.load(nbt.getCompound("oc:node" + index), registries);
            }
            ++index;
        }
    }

    public void saveAdditional(@NotNull CompoundTag nbt, final HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(nbt, registries);
        int index = 0;
        for (Node node : nodes) {
            if (node != null && Objects.equals(node.host(), this)) {
                final CompoundTag nodeNbt = new CompoundTag();
                node.save(nodeNbt, registries);
                nbt.put("oc:node" + index, nodeNbt);
            }
            ++index;
        }
    }
}
