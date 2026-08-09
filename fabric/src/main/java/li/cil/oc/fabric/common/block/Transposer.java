package li.cil.oc.fabric.common.block;

import li.cil.oc.core.impl.common.block.SimpleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class Transposer extends SimpleBlock {
    public Transposer() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 5f));
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.fabric.common.blockentity.Transposer(pos, state);
    }
}
