package li.cil.oc.core.impl.server.machine.luaj;

import li.cil.oc.api.machine.Value;
import li.cil.oc.core.impl.Settings;
import li.cil.repack.org.luaj.vm2.LuaString;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ScalaClosure {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalaClosure.class);

    public static LuaValue wrapClosure(Function<Varargs, Varargs> f) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return f.apply(args);
            }
        };
    }

    public static LuaValue wrapVarArgClosure(Function<Varargs, Varargs> f) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return f.apply(args);
            }
        };
    }

    public static List<Object> toSimpleJavaObjects(Varargs args, int start) {
        List<Object> result = new ArrayList<>();
        for (int i = start; i <= args.narg(); i++) {
            result.add(toSimpleJavaObject(args.arg(i)));
        }
        return result;
    }

    public static Object toSimpleJavaObject(LuaValue value) {
        switch (value.type()) {
            case LuaValue.TBOOLEAN:
                return value.toboolean();
            case LuaValue.TNUMBER:
                return value.todouble();
            case LuaValue.TSTRING:
                if (value instanceof LuaString s) {
                    byte[] bytes = new byte[s.m_length];
                    System.arraycopy(s.m_bytes, s.m_offset, bytes, 0, s.m_length);
                    return bytes;
                }
                return value.tojstring();
            case LuaValue.TTABLE:
                LuaTable table = value.checktable();
                Map<Object, Object> map = new LinkedHashMap<>();
                LuaValue k = LuaValue.NIL;
                while (true) {
                    Varargs n = table.next(k);
                    k = n.arg1();
                    if (k.isnil()) break;
                    map.put(toSimpleJavaObject(k), toSimpleJavaObject(n.arg(2)));
                }
                return map;
            case LuaValue.TUSERDATA:
                return value.touserdata();
            default:
                return null;
        }
    }

    public static LuaValue toLuaValue(Object value) {
        return switch (value) {
            case null -> LuaValue.NIL;
            case Boolean b -> LuaValue.valueOf(b);
            case Byte b -> LuaValue.valueOf(b);
            case Character c -> LuaValue.valueOf(String.valueOf(c));
            case Short s -> LuaValue.valueOf(s);
            case Integer i -> LuaValue.valueOf(i);
            case Long l -> LuaValue.valueOf(l);
            case Float f -> LuaValue.valueOf(f);
            case Double d -> LuaValue.valueOf(d);
            case String s -> LuaValue.valueOf(s);
            case byte[] b -> LuaValue.valueOf(b);
            case Value v when Settings.get().allowUserdata -> LuaValue.userdataOf(v);
            case Object[] a -> toLuaList(java.util.Arrays.asList(a));
            case Map<?, ?> m -> toLuaTable(m);
            case Iterable<?> it -> toLuaList(it);
            default -> {
                if (value.getClass().isArray()) {
                    int len = java.lang.reflect.Array.getLength(value);
                    List<Object> list = new ArrayList<>(len);
                    for (int i = 0; i < len; i++) {
                        list.add(java.lang.reflect.Array.get(value, i));
                    }
                    yield toLuaList(list);
                }
                LOGGER.warn("Tried to push an unsupported value of type to Lua: {}.", value.getClass().getName());
                yield LuaValue.NIL;
            }
        };
    }

    private static LuaValue toLuaList(Iterable<?> iterable) {
        List<LuaValue> values = new ArrayList<>();
        for (Object item : iterable) {
            values.add(toLuaValue(item));
        }
        return LuaValue.listOf(values.toArray(new LuaValue[0]));
    }

    private static LuaValue toLuaTable(Map<?, ?> map) {
        LuaTable table = new LuaTable();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            table.set(toLuaValue(entry.getKey()), toLuaValue(entry.getValue()));
        }
        return table;
    }
}
