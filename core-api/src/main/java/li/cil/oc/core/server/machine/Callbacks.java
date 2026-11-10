package li.cil.oc.core.server.machine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Callbacks {
  private static final Logger LOGGER = LoggerFactory.getLogger(Callbacks.class);

  private static final Map<Class<?>, Map<String, CallbackWrapper>> cache = new WeakHashMap<>();

  private Callbacks() {
  }

  public static void clear() {
    synchronized (cache) {
      cache.clear();
    }
  }

  public static Map<String, CallbackWrapper> apply(Object host) {
    if (host instanceof EnvironmentHost || host instanceof ManagedPeripheral || host instanceof FilteredEnvironment) {
      return Collections.unmodifiableMap(dynamicAnalyze(host));
    }
    Class<?> clazz = host.getClass();
    synchronized (cache) {
      Map<String, CallbackWrapper> result = cache.get(clazz);
      if (result == null) {
        result = dynamicAnalyze(host);
        cache.put(clazz, result);
      }
      return Collections.unmodifiableMap(result);
    }
  }

  private static Map<String, CallbackWrapper> dynamicAnalyze(Object host) {
    Set<String> whitelist = null;
    Map<String, CallbackWrapper> callbacks = new LinkedHashMap<>();
    List<Entry> entries = new ArrayList<>();

    if (host instanceof EnvironmentHost container) {
      for (ManagedEnvironment env : container.environments()) {
        if (env instanceof MethodWhitelist mw) {
          Set<String> ws = new HashSet<>();
          String[] methods = mw.whitelistedMethods();
          if (methods != null) Collections.addAll(ws, methods);
          if (whitelist == null) {
            whitelist = ws;
          } else {
            whitelist.retainAll(ws);
          }
        }
        int priority = env instanceof NamedBlock named ? named.priority() : 0;
        entries.add(new Entry(env, env, priority));
      }
    } else {
      if (host instanceof MethodWhitelist mw) {
        Set<String> ws = new HashSet<>();
        String[] methods = mw.whitelistedMethods();
        if (methods != null) Collections.addAll(ws, methods);
        whitelist = ws;
      }
      int priority = host instanceof NamedBlock named ? named.priority() : 0;
      entries.add(new Entry(host, null, priority));
    }

    final Set<String> finalWhitelist = whitelist;
    java.util.function.Predicate<String> shouldAdd = name -> {
      if (callbacks.containsKey(name)) return false;
      return finalWhitelist == null || finalWhitelist.isEmpty() || finalWhitelist.contains(name);
    };

    entries.sort((a, b) -> Integer.compare(b.priority, a.priority));
    for (Entry entry : entries) {
      process(entry.env, entry.environment, shouldAdd, callbacks);
    }

    return callbacks;
  }

  private static void process(Object env, Object environment, java.util.function.Predicate<String> shouldAdd, Map<String, CallbackWrapper> callbacks) {
    java.util.function.Predicate<String> filter = env instanceof FilteredEnvironment fe
      ? name -> shouldAdd.test(name) && fe.isCallbackEnabled(name)
      : shouldAdd;

    if (env instanceof ManagedPeripheral peripheral) {
      for (String name : peripheral.methods()) {
        if (filter.test(name)) {
          callbacks.put(name, CallbackWrapper.peripheral(name, peripheral));
        }
      }
    }

    staticAnalyze(env.getClass(), environment, filter, callbacks);
  }

  private static void staticAnalyze(Class<?> seed, Object environment, java.util.function.Predicate<String> shouldAdd, Map<String, CallbackWrapper> callbacks) {
    Class<?> c = seed;
    while (c != null && c != Object.class) {
      for (Method method : c.getDeclaredMethods()) {
        Callback annotation = method.getAnnotation(Callback.class);
        if (annotation != null && validCallback(method)) {
          String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
          if (shouldAdd == null || shouldAdd.test(name)) {
            callbacks.put(name, CallbackWrapper.of(method, annotation, environment));
          }
        }
      }
      for (Class<?> iface : c.getInterfaces()) {
        for (Method method : iface.getDeclaredMethods()) {
          Callback annotation = method.getAnnotation(Callback.class);
          if (annotation != null && validCallback(method)) {
            String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
            if (!callbacks.containsKey(name) && (shouldAdd == null || shouldAdd.test(name))) {
              callbacks.put(name, CallbackWrapper.of(method, annotation, environment));
            }
          }
        }
      }
      c = c.getSuperclass();
    }
  }

  private static boolean validCallback(Method method) {
    Class<?>[] params = method.getParameterTypes();
    if (params.length != 2 || params[0] != Context.class || params[1] != Arguments.class) {
      LOGGER.error("Invalid use of Callback annotation on {}.{}: invalid argument types or count.", method.getDeclaringClass().getName(), method.getName());
      return false;
    }
    if (method.getReturnType() != Object[].class) {
      LOGGER.error("Invalid use of Callback annotation on {}.{}: invalid return type.", method.getDeclaringClass().getName(), method.getName());
      return false;
    }
    if (!Modifier.isPublic(method.getModifiers())) {
      LOGGER.error("Invalid use of Callback annotation on {}.{}: method must be public.", method.getDeclaringClass().getName(), method.getName());
      return false;
    }
    return true;
  }

  public static Map<String, CallbackWrapper> fromClass(Class<?> environment) {
    Map<String, CallbackWrapper> callbacks = new LinkedHashMap<>();
    staticAnalyze(environment, null, null, callbacks);
    return callbacks;
  }

  private record Entry(Object env, Object environment, int priority) {
  }
}
