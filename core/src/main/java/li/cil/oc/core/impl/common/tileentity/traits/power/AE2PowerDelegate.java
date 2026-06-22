package li.cil.oc.core.impl.common.tileentity.traits.power;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public interface AE2PowerDelegate {
    void onUpdateEntity(Common tile);

    void onValidate(Common tile);

    void onInvalidate(Common tile);

    void onNeighborChanged(Common tile);

    void readFromNBT(Common tile, CompoundTag nbt);

    void writeToNBT(Common tile, CompoundTag nbt);

    Object getGridNode(Common tile, Direction ignoredSide);
}
