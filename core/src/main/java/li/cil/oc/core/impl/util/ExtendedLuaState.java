package li.cil.oc.core.impl.util;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import li.cil.oc.api.machine.Value;
import li.cil.oc.core.impl.OCSettings;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtendedLuaState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedLuaState.class);

    public static void pushScalaFunction(LuaState lua, final java.util.function.Function<LuaState, Integer> f) {
        lua.pushJavaFunction(f::apply);
    }

    public static void pushValue(LuaState lua, Object value) {
        pushValue(lua, value, new IdentityHashMap<>());
    }

    @SuppressWarnings("unused")
    public static void pushValue(LuaState lua, Object value, IdentityHashMap<Object, Integer> memo) {
        boolean recursive = !memo.isEmpty();
        int oldTop = lua.getTop();
        if (memo.containsKey(value)) {
            lua.pushValue(memo.get(value));
        } else {

            switch (value) {
                case null -> lua.pushNil();
                case Boolean b -> lua.pushBoolean(b);
                case Byte b -> lua.pushInteger(b);
                case Character c -> lua.pushString(String.valueOf(value));
                case Short aShort -> lua.pushInteger(aShort);
                case Integer integer -> lua.pushInteger(integer);
                case Long l -> lua.pushInteger(l);
                case Float v -> lua.pushNumber(v);
                case Double v -> lua.pushNumber(v);
                case String s -> lua.pushString(s);
                case byte[] bytes -> lua.pushByteArray(bytes);
                case float[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case double[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case int[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case short[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case long[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case char[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case boolean[] arr -> pushPrimitiveArray(lua, value, arr, i -> arr[i], arr.length, memo);
                case Object[] arr -> {
                    java.util.List<Map.Entry<Object, Integer>> list = new java.util.ArrayList<>();
                    for (int i = 0; i < arr.length; i++) {
                        list.add(new java.util.AbstractMap.SimpleEntry<>(arr[i], i));
                    }
                    pushList(lua, value, list.iterator(), memo);
                }
                case Value ignored when OCSettings.get().allowUserdata -> lua.pushJavaObjectRaw(value);
                case Map<?, ?> map -> pushTableFromJavaMap(lua, value, map, memo);
                default -> {
                    LOGGER.warn("Tried to push an unsupported value of type to Lua: {}.", value.getClass().getName());
                    lua.pushNil();
                }
            }

            if (!recursive) {
                lua.setTop(oldTop + 1);
            }
        }
    }

    private static void pushPrimitiveArray(LuaState lua, Object obj, Object ignoredArray, java.util.function.IntFunction<Object> get, int length, IdentityHashMap<Object, Integer> memo) {
        java.util.List<Map.Entry<Object, Integer>> list = new java.util.ArrayList<>();
        for (int i = 0; i < length; i++) {
            list.add(new java.util.AbstractMap.SimpleEntry<>(get.apply(i), i));
        }
        pushList(lua, obj, list.iterator(), memo);
    }

    private static void pushList(LuaState lua, Object obj, Iterator<java.util.Map.Entry<Object, Integer>> list, IdentityHashMap<Object, Integer> memo) {        lua.newTable();
        int tableIndex = lua.getTop();
        memo.put(obj, tableIndex);
        while (list.hasNext()) {
            java.util.Map.Entry<Object, Integer> entry = list.next();
            pushValue(lua, entry.getKey(), memo);
            lua.rawSet(tableIndex, entry.getValue() + 1);
        }
        lua.pushValue(tableIndex);
    }

    private static void pushTableFromJavaMap(LuaState lua, Object obj, java.util.Map<?, ?> map, IdentityHashMap<Object, Integer> memo) {
        lua.newTable(0, map.size());
        int tableIndex = lua.getTop();
        memo.put(obj, tableIndex);
        for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key != null) {
                pushValue(lua, key, memo);
                int keyIndex = lua.getTop();
                pushValue(lua, value, memo);
                lua.pushValue(keyIndex);
                lua.insert(-2);
                lua.setTable(tableIndex);
            }
        }
        lua.pushValue(tableIndex);
    }

    public static Object toSimpleJavaObject(LuaState lua, int index) {
        return switch (lua.type(index)) {
            case LuaType.BOOLEAN -> lua.toBoolean(index);
            case LuaType.NUMBER -> {
                if (lua.isInteger(index)) yield lua.toInteger(index);
                yield lua.toNumber(index);
            }
            case LuaType.STRING -> lua.toByteArray(index);
            case LuaType.TABLE -> lua.toJavaObject(index, Map.class);
            case LuaType.USERDATA -> lua.toJavaObjectRaw(index);
            default -> null;
        };
    }

    public static java.util.List<Object> toSimpleJavaObjects(LuaState lua, int start) {
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (int index = start; index <= lua.getTop(); index++) {
            result.add(toSimpleJavaObject(lua, index));
        }
        return result;
    }
}
