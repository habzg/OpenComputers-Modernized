package li.cil.oc.core.impl.common.tileentity.traits.power;

import net.minecraft.core.Direction;

public interface Common {
    @SuppressWarnings("unused")
    double energyThroughput();

    boolean canConnectPower(Direction side);

    @SuppressWarnings("unused")
    double tryChangeBuffer(Direction side, double amount);

    double tryChangeBuffer(Direction side, double amount, boolean doReceive);

    double globalBuffer(Direction side);

    double globalBufferSize(Direction side);

    @SuppressWarnings("unused")
    double globalDemand(Direction side);

    @SuppressWarnings("unused")
    boolean isClient();

    @SuppressWarnings({"unused", "EmptyMethod"})
    boolean isServer();
}
