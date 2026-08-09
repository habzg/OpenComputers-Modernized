package li.cil.oc.fabric.common.block;

import li.cil.oc.fabric.common.blockentity.NetSplitterTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class NetSplitter extends li.cil.oc.core.impl.common.block.NetSplitter {
    public NetSplitter() {
        super();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new NetSplitterTile(pos, state);
    }
}
