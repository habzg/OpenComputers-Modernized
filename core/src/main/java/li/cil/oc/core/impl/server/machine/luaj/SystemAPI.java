package li.cil.oc.core.impl.server.machine.luaj;

import li.cil.oc.core.impl.Settings;
import li.cil.repack.org.luaj.vm2.LuaValue;

public class SystemAPI extends LuaJAPI {
    public SystemAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue system = LuaValue.tableOf();

        system.set("allowBytecode", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(Settings.get().allowBytecode)));

        system.set("allowGC", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(Settings.get().allowGC)));

        system.set("timeout", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(Settings.get().timeout)));

        lua().set("system", system);
    }
}
