package li.cil.oc.fabric.common.blockentity;

import li.cil.oc.core.impl.common.blockentity.Case;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CaseTile extends Case {
    public CaseTile(BlockPos pos, BlockState state, int tier) {
        super(pos, state, tier);
    }
}
