package li.cil.oc.core.impl.server.machine.luac;

@li.cil.oc.api.machine.Architecture.Name("Lua 5.2")
public class NativeLua52Architecture extends NativeLuaArchitecture {
    public NativeLua52Architecture(li.cil.oc.api.machine.Machine machine) {
        super(machine);
    }

    @Override
    public LuaStateFactory factory() {
        return new LuaStateFactory.Lua52();
    }
}
