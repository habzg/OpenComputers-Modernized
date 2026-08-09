package li.cil.oc.neoforge.common.blockentity;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.Case;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CaseTile extends Case {
    public CaseTile(BlockPos pos, BlockState state, int tier) {
        super(pos, state, tier);
    }

    public void setBlockPos(BlockPos pos) {
        this.worldPosition = pos;
    }

    @Override
    public void onDataPacket(@NotNull Connection ignoredNet, @NotNull ClientboundBlockEntityDataPacket packet, HolderLookup.@NotNull Provider provider) {
        var tag = packet.getTag();
        readFromNBTForClient(tag);
        load(tag, provider);
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.handleUpdateTag(tag, provider);
        load(tag, provider);
        tier = tag.getByte(OCSettings.namespace + "tier");
    }
}
