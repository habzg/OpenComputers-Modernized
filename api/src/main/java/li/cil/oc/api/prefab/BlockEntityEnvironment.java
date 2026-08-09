package li.cil.oc.api.prefab;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class BlockEntityEnvironment extends BlockEntity implements Environment {
    protected Node node;

    protected boolean addedToNetwork = false;

    public BlockEntityEnvironment(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Node node() {
        return node;
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

    public void updateEntity() {
        if (!addedToNetwork) {
            addedToNetwork = true;
            Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (node != null) node.remove();
    }

    @Override
    public void loadAdditional(final @NotNull CompoundTag nbt, final HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(nbt, registries);
        if (node != null && node.host() == this) {
            node.load(nbt.getCompound("oc:node"), registries);
        }
    }

    @Override
    public void saveAdditional(final @NotNull CompoundTag nbt, final HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(nbt, registries);
        if (node != null && node.host() == this) {
            final CompoundTag nodeNbt = new CompoundTag();
            node.save(nodeNbt, registries);
            nbt.put("oc:node", nodeNbt);
        }
    }
}
