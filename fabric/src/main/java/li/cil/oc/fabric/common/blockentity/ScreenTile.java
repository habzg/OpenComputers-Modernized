package li.cil.oc.fabric.common.blockentity;

import li.cil.oc.core.impl.common.blockentity.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ScreenTile extends Screen {
    public ScreenTile(BlockPos pos, BlockState state, int tier) {
        super(pos, state, tier);
    }
}
