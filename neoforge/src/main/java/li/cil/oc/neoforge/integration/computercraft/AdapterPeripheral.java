package li.cil.oc.neoforge.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.common.tileentity.Adapter;
import li.cil.oc.core.impl.server.driver.CompoundBlockEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class AdapterPeripheral implements IDynamicPeripheral {
    private final Adapter adapter;
    private final String[] methodNames;
    private final List<ComponentInfo> components;
    private final Map<String, ComponentInfo> componentsByAddress;

    private record ComponentInfo(
            String type, String address,
            Map<String, String> methods,
            Component component, Node adapterNode) {
    }

    public AdapterPeripheral(final Adapter adapter) {
        this.adapter = adapter;
        this.components = collect(adapter);
        this.componentsByAddress = new HashMap<>();
        for (var ci : components) componentsByAddress.put(ci.address, ci);
        this.methodNames = new String[]{"getComponents", "get", "invoke"};
    }

    private static List<ComponentInfo> collect(final Adapter adapter) {
        var components = new ArrayList<ComponentInfo>();
        var adapterNode = adapter.node();
        if (adapterNode == null) return components;

        for (var reachable : adapterNode.reachableNodes()) {
            if (!(reachable instanceof Component component) || component.visibility() == Visibility.None) continue;

            var host = component.host();
            if (host instanceof DriverPeripheral.Environment) continue;

            if (host instanceof CompoundBlockEnvironment cbe) {
                boolean skip = false;
                for (ManagedEnvironment env : cbe.environments()) {
                    if (env instanceof DriverPeripheral.Environment) {
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;
            }

            var methods = new LinkedHashMap<String, String>();
            for (var m : component.methods()) {
                var annotation = component.annotation(m);
                methods.put(m, annotation != null ? annotation.doc() : "");
            }

            components.add(new ComponentInfo(component.name(), component.address(), methods, component, adapterNode));
        }

        return components;
    }

    @Override
    public @NotNull String getType() {
        return "oc_adapter";
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
        return switch (method) {
            case 0 -> getComponents(computer, context, arguments);
            case 1 -> get(computer, context, arguments);
            case 2 -> invoke(computer, context, arguments);
            default -> MethodResult.of();
        };
    }

    @Override
    public boolean equals(final @Nullable IPeripheral other) {
        return other instanceof AdapterPeripheral ap && ap.adapter == adapter;
    }

    private MethodResult getComponents(final IComputerAccess ignoredComputer, final ILuaContext ignoredContext, final IArguments ignoredArgs) {
        var arr = new Object[components.size()];
        for (int i = 0; i < components.size(); i++) {
            var ci = components.get(i);
            var entry = new HashMap<String, Object>();
            entry.put("type", ci.type);
            entry.put("address", ci.address);
            entry.put("methods", new HashMap<>(ci.methods));
            arr[i] = entry;
        }
        return MethodResult.of(arr);
    }

    private MethodResult get(final IComputerAccess ignoredComputer, final ILuaContext ignoredContext, final IArguments args) throws LuaException {
        if (args.count() < 1) throw new LuaException("expected type string");
        var type = args.getString(0);
        for (var ci : components) {
            if (ci.type.equals(type)) {
                var entry = new HashMap<String, Object>();
                entry.put("type", ci.type);
                entry.put("address", ci.address);
                entry.put("methods", new HashMap<>(ci.methods));
                return MethodResult.of(entry);
            }
        }
        return MethodResult.of();
    }

    private MethodResult invoke(
            final IComputerAccess computer,
            final ILuaContext context,
            final IArguments args) throws LuaException {
        if (args.count() < 2) throw new LuaException("expected address and method name");
        var address = args.getString(0);
        var method = args.getString(1);
        var ci = componentsByAddress.get(address);
        if (ci == null) throw new LuaException("no component with address " + address);

        var callArgs = new Object[Math.max(0, args.count() - 2)];
        for (int i = 0; i < callArgs.length; i++) {
            callArgs[i] = convertCCToOCValue(args.get(i + 2));
        }

        try {
            var fakeContext = new AdapterContext(computer, context, ci.adapterNode);
            var result = ci.component.invoke(method, fakeContext, callArgs);
            return convertOCResult(result);
        } catch (Exception e) {
            var msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            throw new LuaException(msg);
        }
    }


    private static Object convertCCToOCValue(final Object value) {
        if (value instanceof String s) return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return value;
    }

    private static MethodResult convertOCResult(final Object result) {
        if (result == null) return MethodResult.of();
        if (result instanceof Object[] arr) {
            var converted = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                converted[i] = convertOCToCCValue(arr[i]);
            }
            return MethodResult.of(converted);
        }
        return MethodResult.of(convertOCToCCValue(result));
    }

    private static Object convertOCToCCValue(final Object value) {
        if (value == null) return null;
        if (value instanceof byte[] b) return new String(b, java.nio.charset.StandardCharsets.UTF_8);
        return value;
    }


    private static class AdapterContext implements Context {
        private final IComputerAccess computer;
        private final Node adapterNode;

        AdapterContext(final IComputerAccess computer, final ILuaContext ignoredContext, final Node adapterNode) {
            this.computer = computer;
            this.adapterNode = adapterNode;
        }

        @Override
        public Node node() {
            return adapterNode;
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public boolean stop() {
            return false;
        }

        @Override
        public boolean canInteract(final String player) {
            return true;
        }

        @Override
        public boolean signal(final String name, final Object... args) {
            var converted = new Object[args != null ? args.length : 0];
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    converted[i] = convertOCToCCValue(args[i]);
                }
            }
            computer.queueEvent(name, converted);
            return true;
        }

        @Override
        public boolean pause(final double seconds) {
            return false;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public boolean start() {
            return false;
        }

        @Override
        public void consumeCallBudget(final double callCost) {
        }
    }
}
