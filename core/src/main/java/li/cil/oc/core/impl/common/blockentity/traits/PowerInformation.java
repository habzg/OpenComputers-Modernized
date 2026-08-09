package li.cil.oc.core.impl.common.blockentity.traits;

public interface PowerInformation extends li.cil.oc.core.impl.common.blockentity.traits.power.Common {
    @SuppressWarnings("unused")
    double globalBuffer();

    void globalBuffer(double value);

    @SuppressWarnings("unused")
    double globalBufferSize();

    void globalBufferSize(double value);

    @SuppressWarnings("unused")
    void updatePowerInformation();
}
