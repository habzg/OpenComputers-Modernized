package li.cil.oc.core.impl.common.block;

import li.cil.oc.core.impl.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Redstone extends RedstoneAware {
    public static BlockEntityType<?> TYPE;

    public Redstone(BlockEntityType<?> blockType) {
        super();
        TYPE = blockType;
    }

    public Redstone() {
        super();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Redstone(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == TYPE ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.blockentity.Redstone) te).updateEntity();
            } catch (Exception e) {
                Log.get().warn("Error in redstone tick", e);
            }
        } : null;
    }
}
