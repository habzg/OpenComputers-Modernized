package li.cil.oc.core.impl.common.tileentity.traits;

import li.cil.oc.api.network.SidedEnvironment;


public interface PowerBalancer extends PowerInformation, SidedEnvironment {
    double globalBuffer();

    void globalBuffer(double value);

    double globalBufferSize();

    void globalBufferSize(double value);

    boolean isServer();

    @SuppressWarnings("unused")
    boolean isConnected();

    @SuppressWarnings("unused")
    void updateEntity() ;
}
