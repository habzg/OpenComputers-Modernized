package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.core.impl.util.BlockInteractionHandler;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WorldAction {
  private static BlockInteractionHandler handler = new BlockInteractionHandler() {
    @Override
    public Player getFakePlayer(Level level, BlockPosition pos) {
      return null;
    }

    @Override
    public boolean mayInteract(Level level, BlockPosition pos, Direction face) {
      return true;
    }

    @Override
    public boolean checkBlockBreak(Level level, BlockPos pos, BlockState state) {
      return false;
    }

    @Override
    public boolean checkItemPickup(Player player, ItemEntity entity) {
      return false;
    }
  };

  public static void setHandler(BlockInteractionHandler h) {
    handler = h;
  }

  public static Player getFakePlayer(Level level, BlockPosition pos) {
    return handler.getFakePlayer(level, pos);
  }

  public static boolean mayInteract(Level level, BlockPosition pos, Direction face) {
    return handler.mayInteract(level, pos, face);
  }

  public static net.minecraft.world.Container itemHandlerContainer(Level level, BlockPos pos, Direction side) {
    return handler.itemHandlerContainer(level, pos, side);
  }

  public static boolean mayInteractEntity(Player player, net.minecraft.world.entity.Entity entity) {
    return handler.mayInteractEntity(player, entity);
  }

  public static boolean checkBlockBreak(Level level, BlockPos pos, BlockState state) {
    return handler.checkBlockBreak(level, pos, state);
  }

  public static boolean checkItemPickup(Player player, ItemEntity entity) {
    return handler.checkItemPickup(player, entity);
  }
}
