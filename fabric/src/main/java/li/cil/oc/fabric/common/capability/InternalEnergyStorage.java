package li.cil.oc.fabric.common.capability;

import li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.integration.util.Power;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;

public final class InternalEnergyStorage implements EnergyStorage {
    private final PowerAcceptor tile;
    private final Direction side;

    public InternalEnergyStorage(final PowerAcceptor tile, final Direction side) {
        this.tile = tile;
        this.side = side;
    }

    @Override
    public long getAmount() {
        return Power.toRF(tile.globalBuffer(side));
    }

    @Override
    public long getCapacity() {
        return Power.toRF(tile.globalBufferSize(side));
    }

    @Override
    public boolean supportsInsertion() {
        return tile.canConnectPower(side);
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        double ocAmount = Power.fromRF((int) Math.min(maxAmount, Integer.MAX_VALUE));
        double received = tile.tryChangeBuffer(side, ocAmount, true);
        if (received > 0) {
            transaction.addCloseCallback((t, result) -> {
                if (result == TransactionContext.Result.ABORTED) {
                    tile.tryChangeBuffer(side, -received, true);
                }
            });
        }
        return Power.toRF(received);
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }
}
