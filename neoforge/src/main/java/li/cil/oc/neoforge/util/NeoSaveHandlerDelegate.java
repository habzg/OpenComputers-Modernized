package li.cil.oc.neoforge.util;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.SaveHandlerDelegate;
import net.minecraft.nbt.CompoundTag;

public class NeoSaveHandlerDelegate extends SaveHandlerDelegate {
    @Override
    public byte[] load(CompoundTag nbt, String name) {
        return li.cil.oc.neoforge.common.SaveHandler.load(nbt, name);
    }

    @Override
    public CompoundTag loadNBT(CompoundTag nbt, String name) {
        return li.cil.oc.neoforge.common.SaveHandler.loadNBT(nbt, name);
    }

    @Override
    public void scheduleSave(MachineHost host, CompoundTag nbt, String name, byte[] data) {
        li.cil.oc.neoforge.common.SaveHandler.scheduleSave(host, nbt, name, data);
    }

    @Override
    public void scheduleSave(EnvironmentHost host, CompoundTag nbt, String name, byte[] data) {
        li.cil.oc.neoforge.common.SaveHandler.scheduleSave(host, nbt, name, data);
    }

    @Override
    public void scheduleSave(BlockPosition pos, CompoundTag nbt, String name, byte[] data) {
        li.cil.oc.neoforge.common.SaveHandler.scheduleSave(pos, nbt, name, data);
    }

    @Override
    public boolean savingForClients() {
        return li.cil.oc.core.impl.common.tileentity.traits.TileEntity.savingForClients;
    }
}
