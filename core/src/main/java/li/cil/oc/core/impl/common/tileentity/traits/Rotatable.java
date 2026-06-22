package li.cil.oc.core.impl.common.tileentity.traits;

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
    void readFromNBTForServer(CompoundTag nbt) ;

    @SuppressWarnings("unused")
    void writeToNBTForServer(CompoundTag nbt);

    @SuppressWarnings("unused")
    void readFromNBTForClient(CompoundTag nbt);

    @SuppressWarnings("unused")
    void writeToNBTForClient(CompoundTag nbt);
}
