package li.cil.oc.core.impl.server.machine.luac;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedLuaState;

public class SystemAPI extends NativeLuaAPI {
    public SystemAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= l.getTop(); i++) {
                if (i > 1) sb.append("  ");
                switch (l.type(i)) {
                    case NIL:
                        sb.append("nil");
                        break;
                    case BOOLEAN:
                        sb.append(l.toBoolean(i));
                        break;
                    case NUMBER:
                        if (l.isInteger(i)) sb.append(l.toInteger(i));
                        else sb.append(l.toNumber(i));
                        break;
                    case STRING:
                        sb.append(l.toString(i));
                        break;
                    case TABLE:
                        sb.append("table");
                        break;
                    case FUNCTION:
                        sb.append("function");
                        break;
                    case THREAD:
                        sb.append("thread");
                        break;
                    case LIGHTUSERDATA:
                    case USERDATA:
                        sb.append("userdata");
                        break;
                }
            }
            System.out.println(sb);
            return 0;
        });
        lua().setGlobal("print");

        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushBoolean(OCSettings.get().allowBytecode);
            return 1;
        });
        lua().setField(-2, "allowBytecode");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushBoolean(OCSettings.get().allowGC);
            return 1;
        });
        lua().setField(-2, "allowGC");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushNumber(OCSettings.get().timeout);
            return 1;
        });
        lua().setField(-2, "timeout");

        lua().setGlobal("system");
    }
}
