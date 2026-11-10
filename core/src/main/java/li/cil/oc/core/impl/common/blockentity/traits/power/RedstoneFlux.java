package li.cil.oc.core.impl.common.blockentity.traits.power;

import li.cil.oc.core.impl.integration.util.Power;
import net.minecraft.core.Direction;

public interface RedstoneFlux extends Common {
  @SuppressWarnings("unused")
  default boolean canConnectEnergy(Direction from) {
    return false;
  }

  @SuppressWarnings("unused")
  default int receiveEnergy(Direction from, int maxReceive, boolean simulate) {
    return Power.toRF(tryChangeBuffer(from, Power.fromRF(maxReceive), !simulate));
  }

  @SuppressWarnings("unused")
  default int getEnergyStored(Direction from) {
    return Power.toRF(globalBuffer(from));
  }

  @SuppressWarnings("unused")
  default int getMaxEnergyStored(Direction from) {
    return Power.toRF(globalBufferSize(from));
  }

  @SuppressWarnings({"SameReturnValue", "unused"})
  default int extractEnergy(Direction from, int maxExtract, boolean simulate) {
    return 0;
  }
}
