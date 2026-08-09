package li.cil.oc.core.impl.common.blockentity.traits.power;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public interface AE2PowerDelegate {
    void onUpdateEntity(Common block);

    void onValidate(Common block);

    void onInvalidate(Common block);

    void onNeighborChanged(Common block);

    void readFromNBT(Common block, CompoundTag nbt);

    void writeToNBT(Common block, CompoundTag nbt);

    Object getGridNode(Common block, Direction ignoredSide);
}
