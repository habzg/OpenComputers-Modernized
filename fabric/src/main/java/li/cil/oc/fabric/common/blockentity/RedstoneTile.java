package li.cil.oc.fabric.common.blockentity;

import li.cil.oc.core.impl.common.blockentity.Redstone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneTile extends Redstone {
    public RedstoneTile(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        shouldUpdateInput = true;
    }
}
