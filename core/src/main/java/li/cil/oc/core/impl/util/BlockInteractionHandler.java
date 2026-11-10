package li.cil.oc.core.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockInteractionHandler {
  Player getFakePlayer(Level level, BlockPosition ignoredPos);

  boolean mayInteract(Level level, BlockPosition pos, Direction face);

  default net.minecraft.world.Container itemHandlerContainer(Level level, BlockPos pos, Direction side) {
    return null;
  }

  default boolean mayInteractEntity(Player player, net.minecraft.world.entity.Entity entity) {
    return true;
  }

  boolean checkBlockBreak(Level level, BlockPos pos, BlockState state);

  boolean checkItemPickup(Player player, ItemEntity entity);
}
