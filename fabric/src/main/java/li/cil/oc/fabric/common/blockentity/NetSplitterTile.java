package li.cil.oc.fabric.common.blockentity;

import li.cil.oc.core.impl.common.blockentity.NetSplitter;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class NetSplitterTile extends NetSplitter implements RenderDataBlockEntity {

    public record NetSplitterRenderData(boolean[] openSides) {
    }

    public NetSplitterTile(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public Object getRenderData() {
        boolean[] openSides = new boolean[6];
        for (Direction dir : Direction.values()) {
            openSides[dir.get3DDataValue()] = isSideOpen(dir);
        }
        return new NetSplitterRenderData(openSides);
    }
}
