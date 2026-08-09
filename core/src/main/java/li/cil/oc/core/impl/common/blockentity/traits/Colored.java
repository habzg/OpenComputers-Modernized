package li.cil.oc.core.impl.common.blockentity.traits;


public interface Colored extends li.cil.oc.api.internal.Colored {
    int color();

    void color(int value);

    @SuppressWarnings("SameReturnValue")
    boolean consumesDye();

    @Override
    @SuppressWarnings("SameReturnValue")
    default boolean controlsConnectivity() {
        return false;
    }

    @SuppressWarnings("unused")
    void onColorChanged();

    @SuppressWarnings("unused")
    void readFromNBTForServer(net.minecraft.nbt.CompoundTag nbt) ;

    @SuppressWarnings("unused")
    void writeToNBTForServer(net.minecraft.nbt.CompoundTag nbt);

    @SuppressWarnings("unused")
    void readFromNBTForClient(net.minecraft.nbt.CompoundTag nbt);

    @SuppressWarnings("unused")
    void writeToNBTForClient(net.minecraft.nbt.CompoundTag nbt);
}
