package li.cil.oc.core.impl.common.tileentity.traits;

public interface PowerInformation extends li.cil.oc.core.impl.common.tileentity.traits.power.Common {
    @SuppressWarnings("unused")
    double globalBuffer();

    void globalBuffer(double value);

    @SuppressWarnings("unused")
    double globalBufferSize();

    void globalBufferSize(double value);

    @SuppressWarnings("unused")
    void updatePowerInformation();
}
