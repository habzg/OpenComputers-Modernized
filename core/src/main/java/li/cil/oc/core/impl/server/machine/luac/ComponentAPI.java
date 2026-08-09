package li.cil.oc.core.impl.server.machine.luac;

import java.util.Map;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Component;
import li.cil.oc.core.impl.util.ExtendedLuaState;

public class ComponentAPI extends NativeLuaAPI {
    public ComponentAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    public void initialize() {
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            synchronized (components()) {
                String filter = l.isString(1) ? l.toString(1) : null;
                boolean exact = !l.isBoolean(2) || l.toBoolean(2);
                l.newTable(0, components().size());
                for (Map.Entry<String, String> entry : components().entrySet()) {
                    String address = entry.getKey();
                    String name = entry.getValue();
                    if (filter == null || (exact ? name.equals(filter) : name.contains(filter))) {
                        l.pushString(address);
                        l.pushString(name);
                        l.rawSet(-3);
                    }
                }
                return 1;
            }
        });
        lua().setField(-2, "list");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            synchronized (components()) {
                String name = components().get(l.checkString(1));
                if (name != null) {
                    l.pushString(name);
                    return 1;
                } else {
                    l.pushNil();
                    l.pushString("no such component");
                    return 2;
                }
            }
        });
        lua().setField(-2, "type");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            synchronized (components()) {
                String address = l.checkString(1);
                String name = components().get(address);
                if (name != null) {
                    l.pushInteger(owner.machine.host().componentSlot(address));
                    return 1;
                } else {
                    l.pushNil();
                    l.pushString("no such component");
                    return 2;
                }
            }
        });
        lua().setField(-2, "slot");

        ExtendedLuaState.pushScalaFunction(lua(), l -> withComponent(l.checkString(1), component -> {
            l.newTable();
            for (Map.Entry<String, Callback> entry : machine.methods(component.host()).entrySet()) {
                String name = entry.getKey();
                Callback annotation = entry.getValue();
                l.pushString(name);
                l.newTable();
                l.pushBoolean(annotation.direct());
                l.setField(-2, "direct");
                l.pushBoolean(annotation.getter());
                l.setField(-2, "getter");
                l.pushBoolean(annotation.setter());
                l.setField(-2, "setter");
                l.rawSet(-3);
            }
            return 1;
        }));
        lua().setField(-2, "methods");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String address = l.checkString(1);
            String method = l.checkString(2);
            Object[] args = ExtendedLuaState.toSimpleJavaObjects(l, 3).toArray();
            return owner.invoke(() -> machine.invoke(address, method, args));
        });
        lua().setField(-2, "invoke");

        ExtendedLuaState.pushScalaFunction(lua(), l -> withComponent(l.checkString(1), component -> {
            String method = l.checkString(2);
            Map<String, Callback> methods = machine.methods(component.host());
            Callback cb = methods.get(method);
            if (cb != null && cb.doc() != null && !cb.doc().isEmpty()) {
                l.pushString(cb.doc());
            } else {
                l.pushNil();
            }
            return 1;
        }));
        lua().setField(-2, "doc");

        lua().setGlobal("component");
    }

    private int withComponent(String address, java.util.function.Function<Component, Integer> f) {
        li.cil.oc.api.network.Node n = node().network().node(address);
        if (n instanceof Component && (((Component) n).canBeSeenFrom(node()) || n == node())) {
            return f.apply((Component) n);
        } else {
            lua().pushNil();
            lua().pushString("no such component");
            return 2;
        }
    }
}
