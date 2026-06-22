package li.cil.oc.core.impl.server.machine.luac;

@li.cil.oc.api.machine.Architecture.Name("Lua 5.3")
public class NativeLua53Architecture extends NativeLuaArchitecture {
    public NativeLua53Architecture(li.cil.oc.api.machine.Machine machine) {
        super(machine);
    }

    @Override
    public LuaStateFactory factory() {
        return new LuaStateFactory.Lua53();
    }
}
