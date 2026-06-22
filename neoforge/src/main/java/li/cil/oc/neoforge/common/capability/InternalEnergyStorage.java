package li.cil.oc.neoforge.common.capability;

import li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.neoforge.integration.util.Power;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class InternalEnergyStorage implements IEnergyStorage {
    private final PowerAcceptor tile;
    private final Direction side;

    public InternalEnergyStorage(final PowerAcceptor tile, final Direction side) {
        this.tile = tile;
        this.side = side;
    }

    @Override
    public int getEnergyStored() {
        return Power.toRF(tile.globalBuffer(side));
    }

    @Override
    public int getMaxEnergyStored() {
        return Power.toRF(tile.globalBufferSize(side));
    }

    @Override
    public boolean canReceive() {
        return tile.canConnectPower(side);
    }

    @Override
    public int receiveEnergy(final int maxReceive, final boolean simulate) {
        return Power.toRF(tile.tryChangeBuffer(side, Power.fromRF(maxReceive), !simulate));
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public int extractEnergy(final int maxExtract, final boolean simulate) {
        return 0;
    }
}
