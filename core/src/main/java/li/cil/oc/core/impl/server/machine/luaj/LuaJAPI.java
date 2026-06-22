package li.cil.oc.core.impl.server.machine.luaj;

import li.cil.oc.core.server.machine.ArchitectureAPI;
import li.cil.repack.org.luaj.vm2.Globals;

public abstract class LuaJAPI extends ArchitectureAPI {
    protected final LuaJLuaArchitecture owner;

    protected LuaJAPI(LuaJLuaArchitecture owner) {
        super(owner.machine);
        this.owner = owner;
    }

    protected Globals lua() {
        return owner.lua;
    }

    public void initialize() {
    }
}
