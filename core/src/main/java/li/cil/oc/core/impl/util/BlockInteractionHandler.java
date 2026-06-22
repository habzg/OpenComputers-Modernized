package li.cil.oc.core.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockInteractionHandler {
    Player getFakePlayer(Level level, BlockPosition pos);

    boolean mayInteract(Level level, BlockPosition pos, Direction face);

    boolean checkBlockBreak(Level level, BlockPos pos, BlockState state);
}
