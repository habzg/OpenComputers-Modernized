package li.cil.oc.core.util;

import java.util.Map;

public final class MapUtils {
    private MapUtils() {
    }

    public static Integer getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static Double getDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static Float getFloat(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.floatValue();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static Long getLong(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static Boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return null;
    }

    public static String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof String s) {
            return s;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unused"})
    public static Map<?, ?> getMap(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map m) {
            return m;
        }
        return null;
    }
}
