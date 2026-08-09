package li.cil.oc.core.impl.server.machine.luaj;

import java.util.Map;
import li.cil.oc.api.network.Component;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;


public class ComponentAPI extends LuaJAPI {
    public ComponentAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue component = LuaValue.tableOf();

        component.set("list", ScalaClosure.wrapVarArgClosure(args -> {
            synchronized (components()) {
                String filter = args.isstring(1) ? args.tojstring(1) : null;
                boolean exact = args.optboolean(2, false);
                LuaValue table = LuaValue.tableOf(0, components().size());
                for (Map.Entry<String, String> entry : components().entrySet()) {
                    String address = entry.getKey();
                    String name = entry.getValue();
                    if (filter == null || (exact ? name.equals(filter) : name.contains(filter))) {
                        table.set(address, name);
                    }
                }
                return table;
            }
        }));

        component.set("type", ScalaClosure.wrapVarArgClosure(args -> {
            synchronized (components()) {
                String name = components().get(args.checkjstring(1));
                if (name != null) {
                    return LuaValue.valueOf(name);
                } else {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
                }
            }
        }));

        component.set("slot", ScalaClosure.wrapVarArgClosure(args -> {
            synchronized (components()) {
                String address = args.checkjstring(1);
                String name = components().get(address);
                if (name != null) {
                    return LuaValue.valueOf(owner.machine.host().componentSlot(address));
                } else {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
                }
            }
        }));

        component.set("methods", ScalaClosure.wrapVarArgClosure(args -> withComponent(args.checkjstring(1), comp -> {
            LuaValue table = LuaValue.tableOf();
            for (Map.Entry<String, li.cil.oc.api.machine.Callback> entry : machine.methods(comp.host()).entrySet()) {
                String name = entry.getKey();
                li.cil.oc.api.machine.Callback annotation = entry.getValue();
                table.set(name, LuaValue.tableOf(new LuaValue[]{
                        LuaValue.valueOf("direct"), LuaValue.valueOf(annotation.direct()),
                        LuaValue.valueOf("getter"), LuaValue.valueOf(annotation.getter()),
                        LuaValue.valueOf("setter"), LuaValue.valueOf(annotation.setter())
                }));
            }
            return table;
        })));

        component.set("invoke", ScalaClosure.wrapVarArgClosure(args -> {
            String address = args.checkjstring(1);
            String method = args.checkjstring(2);
            Object[] params = ScalaClosure.toSimpleJavaObjects(args, 3).toArray();
            return owner.invoke(() -> machine.invoke(address, method, params));
        }));

        component.set("doc", ScalaClosure.wrapVarArgClosure(args -> withComponent(args.checkjstring(1), comp -> {
            String method = args.checkjstring(2);
            Map<String, li.cil.oc.api.machine.Callback> methods = machine.methods(comp.host());
            li.cil.oc.api.machine.Callback cb = methods.get(method);
            String doc = cb != null ? cb.doc() : null;
            if (doc != null && !doc.isEmpty()) return LuaValue.valueOf(doc);
            return LuaValue.NIL;
        })));

        lua().set("component", component);
    }

    private Varargs withComponent(String address, java.util.function.Function<Component, Varargs> f) {
        li.cil.oc.api.network.Node n = node().network().node(address);
        if (n instanceof Component && (((Component) n).canBeSeenFrom(node()) || n == node())) {
            return f.apply((Component) n);
        } else {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
        }
    }
}
