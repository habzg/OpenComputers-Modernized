package li.cil.oc.neoforge.common.block;

import li.cil.oc.neoforge.common.blockentity.PrintNeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Print extends li.cil.oc.core.impl.common.block.Print {
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PrintNeoForge(pos, state);
    }
}
