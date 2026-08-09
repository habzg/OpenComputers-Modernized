package li.cil.oc.core.impl.common.blockentity.traits.power;

import net.minecraft.core.Direction;

public interface RedstoneFlux extends Common {
    @SuppressWarnings("unused")
    default boolean canConnectEnergy(Direction from) {
        return false;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    default int receiveEnergy(Direction from, int maxReceive, boolean simulate) {
        return 0;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    default int getEnergyStored(Direction from) {
        return 0;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    default int getMaxEnergyStored(Direction from) {
        return 0;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    default int extractEnergy(Direction from, int maxExtract, boolean simulate) {
        return 0;
    }
}
