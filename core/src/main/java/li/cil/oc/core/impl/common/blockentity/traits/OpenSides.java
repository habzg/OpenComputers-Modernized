package li.cil.oc.core.impl.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;


public interface OpenSides {
    @SuppressWarnings("unused")
    boolean[] openSides();

    @SuppressWarnings("unused")
    void openSides(boolean[] value);

    @SuppressWarnings("unused")
    boolean isSideOpen(Direction side);

    @SuppressWarnings("unused")
    void setSideOpen(Direction side, boolean value) ;

    @SuppressWarnings("unused")
    byte compressSides();

    @SuppressWarnings("unused")
    void uncompressSides(byte value);

    @SuppressWarnings("unused")
    void readFromNBTForServer(CompoundTag nbt) ;

    @SuppressWarnings("unused")
    void writeToNBTForServer(CompoundTag nbt);

    @SuppressWarnings("unused")
    void readFromNBTForClient(CompoundTag nbt);

    @SuppressWarnings("unused")
    void writeToNBTForClient(CompoundTag nbt);
}
