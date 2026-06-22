package li.cil.oc.core.impl.server.machine.luac;

import li.cil.oc.core.server.machine.ArchitectureAPI;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public abstract class NativeLuaAPI extends ArchitectureAPI {
    protected final NativeLuaArchitecture owner;

    protected NativeLuaAPI(NativeLuaArchitecture owner) {
        super(owner.machine);
        this.owner = owner;
    }

    protected LuaState lua() {
        return owner.lua;
    }

    public void initialize() {
    }

    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
    }

    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
    }
}
