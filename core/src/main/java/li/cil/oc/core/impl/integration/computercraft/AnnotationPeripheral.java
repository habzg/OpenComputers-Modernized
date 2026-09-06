package li.cil.oc.core.impl.integration.computercraft;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaFunction;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class AnnotationPeripheral implements IDynamicPeripheral {
  private final IPeripheral peripheral;
  private final Map<String, Method> methods;
  private final String[] methodNames;
  private final Map<String, Integer> dynamicMethods;

  public AnnotationPeripheral(final IPeripheral peripheral) {
    this.peripheral = peripheral;
    this.methods = discoverMethods(peripheral.getClass());

    this.dynamicMethods = new LinkedHashMap<>();
    if (peripheral instanceof IDynamicPeripheral dynamic) {
      String[] dynamicNames = dynamic.getMethodNames();
      for (int i = 0; i < dynamicNames.length; i++) {
        this.dynamicMethods.put(dynamicNames[i], i);
      }
    }

    LinkedHashSet<String> allMethodNames = new LinkedHashSet<>();
    allMethodNames.addAll(methods.keySet());
    allMethodNames.addAll(dynamicMethods.keySet());
    this.methodNames = allMethodNames.toArray(new String[0]);
  }

  private static Map<String, Method> discoverMethods(final Class<?> clazz) {
    var result = new LinkedHashMap<String, Method>();
    collectMethods(clazz, result);
    return result;
  }

  private static void collectMethods(final Class<?> clazz, final Map<String, Method> result) {
    if (clazz == null || clazz == Object.class) return;

    for (var method : clazz.getDeclaredMethods()) {
      var annotation = method.getAnnotation(LuaFunction.class);
      if (annotation == null) continue;
      var mod = method.getModifiers();
      if (!Modifier.isPublic(mod) || !Modifier.isFinal(mod)) continue;

      var names = annotation.value();
      if (names.length == 0) {
        names = new String[]{method.getName()};
      }
      for (var name : names) {
        result.putIfAbsent(name, method);
      }
    }

    collectMethods(clazz.getSuperclass(), result);
    for (var iface : clazz.getInterfaces()) {
      collectMethods(iface, result);
    }
  }

  @Override
  public @NotNull String getType() {
    return peripheral.getType();
  }

  @Override
  public @NotNull Set<String> getAdditionalTypes() {
    return peripheral.getAdditionalTypes();
  }

  @Override
  public String @NotNull [] getMethodNames() {
    return methodNames;
  }

  @Override
  public @NotNull MethodResult callMethod(
    final @NotNull IComputerAccess computer,
    final @NotNull ILuaContext context,
    final int method,
    final @NotNull IArguments arguments) throws LuaException {
    var name = methodNames[method];

    // Method declared directly on DynamicPeripheral
    if (!methods.containsKey(name) && peripheral instanceof IDynamicPeripheral dynamic) {
      var dynIndex = dynamicMethods.get(name);
      if (dynIndex != null) {
        return dynamic.callMethod(computer, context, dynIndex, arguments);
      }
    }

    // Annotation-based method
    var javaMethod = methods.get(name);
    if (javaMethod == null) return MethodResult.of();

    var paramTypes = javaMethod.getParameterTypes();
    var genericParamTypes = javaMethod.getGenericParameterTypes();
    var callArgs = new Object[paramTypes.length];

    var argIdx = 0;
    for (int i = 0; i < paramTypes.length; i++) {
      var type = paramTypes[i];
      if (type == IComputerAccess.class) {
        callArgs[i] = computer;
      } else if (type == ILuaContext.class) {
        callArgs[i] = context;
      } else if (type == IArguments.class) {
        callArgs[i] = arguments;
      } else if (type == Coerced.class) {
        java.lang.reflect.Type actualType = Object.class;
        if (genericParamTypes[i] instanceof ParameterizedType pt) {
          actualType = pt.getActualTypeArguments()[0];
        }
        callArgs[i] = new Coerced<>(coerceValue(arguments, argIdx++, actualType, true));
      } else if (type == Optional.class) {
        java.lang.reflect.Type actualType = Object.class;
        if (genericParamTypes[i] instanceof ParameterizedType pt) {
          actualType = pt.getActualTypeArguments()[0];
        }
        if (argIdx < arguments.count()) {
          callArgs[i] = Optional.ofNullable(coerceValue(arguments, argIdx++, actualType, false));
        } else {
          callArgs[i] = Optional.empty();
        }
      } else if (type == OptionalInt.class) {
        if (argIdx < arguments.count()) {
          callArgs[i] = OptionalInt.of((int) arguments.getLong(argIdx++));
        } else {
          callArgs[i] = OptionalInt.empty();
        }
      } else if (type == OptionalLong.class) {
        if (argIdx < arguments.count()) {
          callArgs[i] = OptionalLong.of(arguments.getLong(argIdx++));
        } else {
          callArgs[i] = OptionalLong.empty();
        }
      } else if (type == OptionalDouble.class) {
        if (argIdx < arguments.count()) {
          callArgs[i] = OptionalDouble.of(arguments.getDouble(argIdx++));
        } else {
          callArgs[i] = OptionalDouble.empty();
        }
      } else {
        callArgs[i] = coerceValue(arguments, argIdx++, type, false);
      }
    }

    try {
      var returnValue = javaMethod.invoke(peripheral, callArgs);
      return convertResult(returnValue, javaMethod.getReturnType());
    } catch (InvocationTargetException e) {
      var cause = e.getCause();
      if (cause instanceof LuaException le) throw le;
      throw new LuaException(cause != null ? cause.getMessage() : "unknown error");
    } catch (Exception e) {
      throw new LuaException(e.getMessage());
    }
  }

  private static Object coerceValue(
    final IArguments args, final int idx, final Type targetType, final boolean coerced) throws LuaException {
    if (targetType == String.class) {
      return coerced ? args.getStringCoerced(idx) : args.getString(idx);
    } else if (targetType == double.class || targetType == Double.class) {
      return args.getDouble(idx);
    } else if (targetType == float.class || targetType == Float.class) {
      return (float) args.getDouble(idx);
    } else if (targetType == int.class || targetType == Integer.class) {
      return (int) args.getLong(idx);
    } else if (targetType == long.class || targetType == Long.class) {
      return args.getLong(idx);
    } else if (targetType == boolean.class || targetType == Boolean.class) {
      return args.getBoolean(idx);
    } else if (targetType == byte[].class) {
      return toBytes(args.getBytes(idx));
    } else if (targetType == Map.class) {
      return args.getTable(idx);
    } else {
      return args.get(idx);
    }
  }

  private static byte[] toBytes(final ByteBuffer buffer) {
    var result = new byte[buffer.remaining()];
    buffer.get(result);
    return result;
  }

  private static MethodResult convertResult(final Object value, final Class<?> returnType) {
    if (returnType == void.class) return MethodResult.of();
    switch (value) {
      case null -> {
        return MethodResult.of();
      }
      case MethodResult mr -> {
        if (mr.getCallback() != null) return mr;
        Object[] inner = mr.getResult();
        if (inner == null || inner.length == 0) return MethodResult.of();
        if (inner.length == 1) return MethodResult.of(inner[0]);
        return MethodResult.of(inner);
      }
      case Object[] arr -> {
        return MethodResult.of(arr);
      }
      case Map<?, ?> map -> {
        return MethodResult.of(convertMapValues(map));
      }
      default -> {
      }
    }
    return MethodResult.of(value);
  }

  private static Map<Object, Object> convertMapValues(Map<?, ?> map) {
    Map<Object, Object> result = new LinkedHashMap<>();
    for (var entry : map.entrySet()) {
      Object key = entry.getKey();
      Object val = entry.getValue();
      if (val instanceof Map<?, ?> nested) {
        result.put(key, convertMapValues(nested));
      } else if (val instanceof ILuaFunction func) {
        result.put(key, wrapCCTLuaFunction(func));
      } else {
        result.put(key, val);
      }
    }
    return result;
  }

  private static li.cil.oc.core.impl.server.machine.luaj.ScalaClosure.LuaCallable wrapCCTLuaFunction(ILuaFunction func) {
    return args -> {
      var iargs = createCCTArguments(args);
      var result = func.call(iargs);
      if (result.getCallback() != null) {
        throw new UnsupportedOperationException("async not supported via OC bridge");
      }
      return result.getResult();
    };
  }

  private static IArguments createCCTArguments(Object[] args) {
    return new IArguments() {
      @Override public int count() { return args.length; }
      @Override public Object get(int index) {
        if (index < 0 || index >= args.length) return null;
        return args[index];
      }
      @Override public @NotNull String getType(int index) {
        Object val = get(index);
        return switch (val) {
          case null -> "nil";
          case Boolean b -> "boolean";
          case Number number -> "number";
          case String s -> "string";
          //noinspection rawtypes
          case Map map -> "table";
          default -> "object";
        };
      }
      @Override public @NotNull IArguments drop(int count) {
        if (count <= 0) return this;
        if (count >= args.length) return createCCTArguments(new Object[0]);
        Object[] newArgs = new Object[args.length - count];
        System.arraycopy(args, count, newArgs, 0, newArgs.length);
        return createCCTArguments(newArgs);
      }
    };
  }

  @Override
  public void attach(final @NotNull IComputerAccess computer) {
    peripheral.attach(computer);
  }

  @Override
  public void detach(final @NotNull IComputerAccess computer) {
    peripheral.detach(computer);
  }

  @Override
  public boolean equals(final IPeripheral other) {
    return peripheral.equals(other);
  }

  @Override
  public Object getTarget() {
    return peripheral.getTarget();
  }
}
