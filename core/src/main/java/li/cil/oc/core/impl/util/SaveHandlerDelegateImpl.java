package li.cil.oc.core.impl.util;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import net.minecraft.nbt.CompoundTag;

public class SaveHandlerDelegateImpl extends SaveHandlerDelegate {
    @Override
    public byte[] load(CompoundTag nbt, String name) {
        return StateSaveManager.load(nbt, name);
    }

    @Override
    public CompoundTag loadNBT(CompoundTag nbt, String name) {
        return StateSaveManager.loadNBT(nbt, name);
    }

    @Override
    public void scheduleSave(MachineHost host, CompoundTag nbt, String name, byte[] data) {
        StateSaveManager.scheduleSave(host, nbt, name, data);
    }

    @Override
    public void scheduleSave(EnvironmentHost host, CompoundTag nbt, String name, byte[] data) {
        StateSaveManager.scheduleSave(host, nbt, name, data);
    }

    @Override
    public void scheduleSave(BlockPosition pos, CompoundTag nbt, String name, byte[] data) {
        StateSaveManager.scheduleSave(pos, nbt, name, data);
    }

    @Override
    public boolean savingForClients() {
        return BlockEntity.savingForClients;
    }
}
