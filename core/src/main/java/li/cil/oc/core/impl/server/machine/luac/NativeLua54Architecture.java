package li.cil.oc.core.impl.server.machine.luac;

@li.cil.oc.api.machine.Architecture.Name("Lua 5.4")
public class NativeLua54Architecture extends NativeLuaArchitecture {
    public NativeLua54Architecture(li.cil.oc.api.machine.Machine machine) {
        super(machine);
    }

    @Override
    public LuaStateFactory factory() {
        return new LuaStateFactory.Lua54();
    }
}
