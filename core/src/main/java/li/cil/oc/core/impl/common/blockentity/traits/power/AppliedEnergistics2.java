package li.cil.oc.core.impl.common.blockentity.traits.power;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public interface AppliedEnergistics2 extends Common {
    default void ae2UpdateEntity() {
        var d = AE2Power.delegate();
        if (d != null) d.onUpdateEntity(this);
    }

    default void ae2Validate() {
        var d = AE2Power.delegate();
        if (d != null) d.onValidate(this);
    }

    default void ae2Invalidate() {
        var d = AE2Power.delegate();
        if (d != null) d.onInvalidate(this);
    }

    default void ae2OnNeighborChanged() {
        var d = AE2Power.delegate();
        if (d != null) d.onNeighborChanged(this);
    }

    default void ae2ReadFromNBT(CompoundTag nbt) {
        var d = AE2Power.delegate();
        if (d != null) d.readFromNBT(this, nbt);
    }

    default void ae2WriteToNBT(CompoundTag nbt) {
        var d = AE2Power.delegate();
        if (d != null) d.writeToNBT(this, nbt);
    }

    @SuppressWarnings("unused")
    default Object getGridNode(Direction side) {
        var d = AE2Power.delegate();
        if (d != null) return d.getGridNode(this, side);
        return null;
    }
}
