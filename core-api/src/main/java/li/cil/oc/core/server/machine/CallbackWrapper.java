package li.cil.oc.core.server.machine;

import li.cil.oc.api.network.DocumentedPeripheral;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;

public class CallbackWrapper {
    private final Method method;
    private final Callback annotation;
    private final Invoker invoker;

    private CallbackWrapper(Method method, Callback annotation, Invoker invoker) {
        this.method = method;
        this.annotation = annotation;
        this.invoker = invoker;
    }

    public static CallbackWrapper of(Method method, Callback annotation) {
        return new CallbackWrapper(method, annotation, (instance, args) -> {
            try {
                return (Object[]) method.invoke(instance, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException r) throw r;
                if (cause instanceof Error) throw (Error) cause;
                throw new RuntimeException(cause);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CallbackWrapper peripheral(String name, ManagedPeripheral peripheral) {
        String doc = peripheral instanceof DocumentedPeripheral documented ? documented.doc(name) : "";
        return new CallbackWrapper(null, createAnnotation(name, doc), (instance, args) ->
                peripheral.invoke(name, (Context) args[0], (Arguments) args[1])
        );
    }

    public Method method() {
        return method;
    }

    public Callback annotation() {
        return annotation;
    }

    public Object[] apply(Object instance, Object... args) {
        return invoker.call(instance, args);
    }

    @FunctionalInterface
    private interface Invoker {
        Object[] call(Object instance, Object... args) ;
    }

    private static Callback createAnnotation(final String name, final String doc) {
        return (Callback) Proxy.newProxyInstance(
                Callback.class.getClassLoader(),
                new Class<?>[]{Callback.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "value" -> name;
                    case "direct" -> true;
                    case "limit" -> 100;
                    case "doc" -> doc;
                    case "getter", "setter" -> false;
                    case "annotationType" -> Callback.class;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> name.hashCode();
                    case "toString" -> "@Callback(" + name + ")";
                    default -> null;
                }
        );
    }
}
