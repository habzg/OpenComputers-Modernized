package li.cil.oc.core.impl.util;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.nbt.CompoundTag;

public abstract class SaveHandlerDelegate {
    private static SaveHandlerDelegate instance;

    public static void setInstance(SaveHandlerDelegate inst) {
        instance = inst;
    }

    public static SaveHandlerDelegate get() {
        return instance;
    }

    public abstract byte[] load(CompoundTag nbt, String name);

    public abstract CompoundTag loadNBT(CompoundTag nbt, String name);

    public abstract void scheduleSave(MachineHost host, CompoundTag nbt, String name, byte[] data);

    public abstract void scheduleSave(EnvironmentHost host, CompoundTag nbt, String name, byte[] data);

    public abstract void scheduleSave(BlockPosition pos, CompoundTag nbt, String name, byte[] data);

    public abstract boolean savingForClients();

    public static CompoundTag loadNBTFrom(CompoundTag nbt, String name) {
        return get().loadNBT(nbt, name);
    }

    public static void schedule(EnvironmentHost host, CompoundTag nbt, String name, byte[] data) {
        get().scheduleSave(host, nbt, name, data);
    }
}
