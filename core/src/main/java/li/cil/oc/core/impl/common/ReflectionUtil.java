package li.cil.oc.core.impl.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ReflectionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtil.class);

    private ReflectionUtil() {
    }

    public static Method getStaticMethod(String name, Class<?>... signature) {
        try {
            int nameSplit = name.lastIndexOf('.');
            String className = name.substring(0, nameSplit);
            String methodName = name.substring(nameSplit + 1);
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, signature);
            if (!Modifier.isStatic(method.getModifiers()))
                throw new IllegalArgumentException("Method " + name + " is not static.");
            return method;
        } catch (Throwable e) {
            LOGGER.warn("Failed to get static method: {}", name, e);
            return null;
        }
    }

    public static Object tryInvokeStatic(Method method, Object defaultValue, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (Throwable t) {
            LOGGER.error("Error invoking callback {}.{}", method.getDeclaringClass().getCanonicalName(), method.getName(), t);
            return defaultValue;
        }
    }
}
