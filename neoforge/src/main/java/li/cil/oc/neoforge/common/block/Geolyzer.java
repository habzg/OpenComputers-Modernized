package li.cil.oc.neoforge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Geolyzer extends SimpleBlock {
    public Geolyzer() {
        super();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.neoforge.common.tileentity.Geolyzer(pos, state);
    }
}
