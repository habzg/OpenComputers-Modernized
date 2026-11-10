package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public class Wrench extends DelegateItem implements li.cil.oc.api.internal.Wrench, ItemUseFirst {

  public Wrench(Properties properties) {
    super(properties.stacksTo(1));
  }

  /** {@link useFirst} */
  @Override
  public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
    return InteractionResult.PASS;
  }

  @Override
  public @NotNull InteractionResult useFirst(@NotNull UseOnContext context) {
    var world = context.getLevel();
    var pos = context.getClickedPos();
    var player = context.getPlayer();
    if (player == null) return InteractionResult.PASS;
    if (!world.isClientSide && (player.isSpectator() || !world.mayInteract(player, pos))) {
      return InteractionResult.PASS;
    }
    if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return InteractionResult.PASS;
    var state = world.getBlockState(pos);
    if (!rotate(world, pos, context.getClickedFace(), state)) return InteractionResult.PASS;
    world.blockUpdated(pos, state.getBlock());
    player.swing(context.getHand());
    return InteractionResult.SUCCESS;
  }

  private static boolean rotate(Level world, BlockPos pos, Direction axis, BlockState state) {
    if (world.getBlockEntity(pos) instanceof Rotatable rotatable) {
      return rotatable.rotateAround(axis);
    }
    return rotateBlock(world, pos, axis, state);
  }

  private static boolean rotateBlock(Level world, BlockPos pos, Direction axis, BlockState state) {
    if (state.getBlock() instanceof BedBlock || state.getBlock() instanceof PistonHeadBlock) {
      return false;
    }
    for (var property : state.getProperties()) {
      if (!property.getName().equals("facing") && !property.getName().equals("rotation")) continue;
      if (property.getValueClass() != Direction.class) continue;
      @SuppressWarnings("unchecked")
      var facingProperty = (Property<Direction>) property;
      var facing = state.getValue(facingProperty);
      var valid = facingProperty.getPossibleValues();
      BlockState newState;
      if (valid.size() == 4 && !valid.contains(Direction.UP) && !valid.contains(Direction.DOWN)) {
        newState = state.setValue(facingProperty, facing.getClockWise());
      } else {
        var rotated = rotateAround(facing, axis);
        newState = valid.contains(rotated) ? state.setValue(facingProperty, rotated) : state.cycle(facingProperty);
      }
      world.setBlock(pos, newState, 3);
      return true;
    }
    return false;
  }

  private static Direction rotateAround(Direction facing, Direction axis) {
    return switch (axis.getAxis()) {
      case X -> facing == Direction.WEST || facing == Direction.EAST ? facing : switch (facing) {
        case NORTH -> Direction.SOUTH;
        case SOUTH -> Direction.NORTH;
        case UP -> Direction.DOWN;
        default -> Direction.UP;
      };
      case Y -> facing == Direction.UP || facing == Direction.DOWN ? facing : facing.getClockWise();
      case Z -> facing == Direction.NORTH || facing == Direction.SOUTH ? facing : switch (facing) {
        case WEST -> Direction.EAST;
        case EAST -> Direction.WEST;
        case UP -> Direction.DOWN;
        default -> Direction.UP;
      };
    };
  }

  @Override
  public boolean useWrenchOnBlock(Player player, Level world, BlockPos pos, boolean simulate) {
    if (!simulate) {
      player.swing(InteractionHand.MAIN_HAND);
    }
    return true;
  }
}
