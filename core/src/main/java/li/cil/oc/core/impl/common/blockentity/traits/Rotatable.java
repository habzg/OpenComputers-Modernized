package li.cil.oc.core.impl.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;


public interface Rotatable extends RotationAware {
  Direction facing();

  default Direction[] validFacings() {
    return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
  }

  default void setFromFacing(Direction value) {
    facing(value);
  }

  default void setFromEntityPitchAndYaw(net.minecraft.world.entity.Entity entity) {
  }

  @SuppressWarnings("unused")
  default void invertRotation() {
  }

  default boolean rotateAround(Direction axis) {
    if (axis != Direction.UP && axis != Direction.DOWN) return false;
    var current = facing();
    if (current == Direction.UP || current == Direction.DOWN) {
      var currentYaw = yaw();
      var newYaw = axis == Direction.UP ? currentYaw.getClockWise() : currentYaw.getCounterClockWise();
      if (newYaw == currentYaw) return false;
      return trySetPitchYaw(current, newYaw);
    }
    var newFacing = axis == Direction.UP ? current.getClockWise() : current.getCounterClockWise();
    if (newFacing == current) return false;
    facing(newFacing);
    return true;
  }

  default boolean trySetPitchYaw(Direction pitch, Direction yaw) {
    return false;
  }

  void facing(Direction value);

  Direction toLocal(Direction global);

  Direction toGlobal(Direction local);

  @SuppressWarnings("unused")
  void onRotationChanged();

  default Direction pitch() {
    return switch (facing()) {
      case DOWN -> Direction.DOWN;
      case UP -> Direction.UP;
      default -> Direction.NORTH;
    };
  }

  default Direction yaw() {
    return switch (facing()) {
      case DOWN, UP -> Direction.NORTH;
      default -> facing();
    };
  }

  @SuppressWarnings("unused")
  default void pitch(Direction value) {
  }

  @SuppressWarnings("unused")
  default void yaw(Direction value) {
  }

  @SuppressWarnings("unused")
  void readFromNBTForServer(CompoundTag nbt);

  @SuppressWarnings("unused")
  void writeToNBTForServer(CompoundTag nbt);

  @SuppressWarnings("unused")
  void readFromNBTForClient(CompoundTag nbt);

  @SuppressWarnings("unused")
  void writeToNBTForClient(CompoundTag nbt);
}
