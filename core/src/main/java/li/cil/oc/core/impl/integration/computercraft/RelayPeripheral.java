package li.cil.oc.core.impl.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.Relay;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class RelayPeripheral implements IDynamicPeripheral {
    private final Relay relay;
    private final Map<String, MethodDispatcher> methods = new HashMap<>();
    private final String[] methodNames;

    @FunctionalInterface
    private interface MethodDispatcher {
        MethodResult apply(IComputerAccess computer, ILuaContext context, IArguments args) throws LuaException;
    }

    public RelayPeripheral(Relay relay) {
        this.relay = relay;

        methods.put("open", (computer, context, args) -> {
            var port = checkPort(args, 0);
            var ports = relay.openPorts.computeIfAbsent(computer, k -> new java.util.HashSet<>());
            if (ports.size() >= 128) throw new IllegalArgumentException("too many open channels");
            return MethodResult.of(ports.add(port));
        });
        methods.put("isOpen", (computer, context, args) -> {
            var port = checkPort(args, 0);
            return MethodResult.of(relay.openPorts.getOrDefault(computer, java.util.Set.of()).contains(port));
        });
        methods.put("close", (computer, context, args) -> {
            var port = checkPort(args, 0);
            var ports = relay.openPorts.get(computer);
            return MethodResult.of(ports != null && ports.remove(port));
        });
        methods.put("closeAll", (computer, context, args) -> {
            var ports = relay.openPorts.get(computer);
            if (ports != null) ports.clear();
            return MethodResult.of();
        });
        methods.put("transmit", (computer, context, args) -> {
            var sendPort = checkPort(args, 0);
            var answerPort = checkPort(args, 1);
            var dataLength = args.count() - 2;
            var data = new Object[dataLength + 1];
            for (int i = 0; i < dataLength; i++) data[i] = args.get(i + 2);
            data[dataLength] = answerPort;
            var address = "cc" + computer.getID() + "_" + computer.getAttachmentName();
            var packet = li.cil.oc.api.Network.newPacket(address, null, sendPort, data);
            return MethodResult.of(relay.tryEnqueuePacket(null, packet));
        });
        methods.put("isWireless", (computer, context, args) -> MethodResult.of(false));
        methods.put("callRemote", (computer, context, args) -> {
            var address = checkString(args, 0);
            var method = checkString(args, 1);
            var remaining = new Object[args.count() - 2];
            for (int i = 0; i < remaining.length; i++) remaining[i] = args.get(i + 2);
            for (var component : visibleComponents()) {
                if (component.address().equals(address)) {
                    var fakeContext = new CCContext(computer, context);
                    try {
                        var result = component.invoke(method, fakeContext, remaining);
                        return MethodResult.of(result);
                    } catch (Exception e) {
                        throw new LuaException(e.getMessage());
                    }
                }
            }
            return MethodResult.of();
        });
        methods.put("getMethodsRemote", (computer, context, args) -> {
            var address = checkString(args, 0);
            for (var component : visibleComponents()) {
                if (component.address().equals(address)) {
                    var methodsList = component.methods().toArray(new String[0]);
                    var map = new java.util.HashMap<Integer, String>();
                    for (int i = 0; i < methodsList.length; i++) map.put(i + 1, methodsList[i]);
                    return MethodResult.of(map);
                }
            }
            return MethodResult.of();
        });
        methods.put("getNamesRemote", (computer, context, args) -> {
            var components = visibleComponents();
            var map = new java.util.HashMap<Integer, String>();
            for (int i = 0; i < components.length; i++) map.put(i + 1, components[i].address());
            return MethodResult.of(map);
        });
        methods.put("getTypeRemote", (computer, context, args) -> {
            var address = checkString(args, 0);
            for (var component : visibleComponents()) {
                if (component.address().equals(address)) {
                    return MethodResult.of(component.name());
                }
            }
            return MethodResult.of();
        });
        methods.put("isPresentRemote", (computer, context, args) -> {
            var address = checkString(args, 0);
            for (var component : visibleComponents()) {
                if (component.address().equals(address)) {
                    return MethodResult.of(true);
                }
            }
            return MethodResult.of(false);
        });
        methods.put("isAccessPoint", (computer, context, args) -> MethodResult.of(relay.isWirelessEnabled()));
        methods.put("isTunnel", (computer, context, args) -> MethodResult.of(relay.isLinkedEnabled()));
        methods.put("maxPacketSize", (computer, context, args) -> MethodResult.of(OCSettings.get().maxNetworkPacketSize));

        var keys = methods.keySet().toArray(new String[0]);
        java.util.Arrays.sort(keys);
        methodNames = keys;
    }

    @Override
    public @NotNull String getType() {
        return "modem";
    }

    @Override
    public void attach(@NotNull IComputerAccess computer) {
        relay.computers.add(computer);
        relay.openPorts.put(computer, new java.util.HashSet<>());
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        relay.computers.remove(computer);
        relay.openPorts.remove(computer);
    }

    @Override
    public String @NotNull [] getMethodNames() {
        return methodNames;
    }

    @Override
    public @NotNull MethodResult callMethod(@NotNull IComputerAccess computer, @NotNull ILuaContext context, int method, @NotNull IArguments arguments) throws LuaException {
        try {
            var dispatcher = methods.get(methodNames[method]);
            if (dispatcher != null) return dispatcher.apply(computer, context, arguments);
            return MethodResult.of();
        } catch (LuaException e) {
            throw e;
        } catch (Throwable t) {
            throw new LuaException(t.getMessage());
        }
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof RelayPeripheral rp && rp.relay == relay;
    }

    private int checkPort(IArguments args, int index) throws LuaException {
        if (args.count() <= index) throw new LuaException("bad argument #" + (index + 1) + " (number expected)");
        var port = args.getInt(index);
        if (port < 0 || port > 0xFFFF)
            throw new LuaException("bad argument #" + (index + 1) + " (number in [1, 65535] expected)");
        return port;
    }

    private String checkString(IArguments args, int index) throws LuaException {
        if (args.count() <= index) throw new LuaException("bad argument #" + (index + 1) + " (string expected)");
        return args.getString(index);
    }

    private Component[] visibleComponents() {
        var list = new java.util.ArrayList<Component>();
        for (var side : Direction.values()) {
            var node = relay.sidedNode(side);
            if (node != null) {
                for (var reachable : node.reachableNodes()) {
                    if (reachable instanceof Component c && c.visibility() != Visibility.None) {
                        list.add(c);
                    }
                }
            }
        }
        return list.toArray(new Component[0]);
    }

    private class CCContext implements li.cil.oc.api.machine.Context {
        private final IComputerAccess computer;

        CCContext(IComputerAccess computer, ILuaContext ignoredContext) {
            this.computer = computer;
        }

        @Override
        public li.cil.oc.api.network.Node node() {
            return relay.node();
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
        public boolean canInteract(String player) {
            return true;
        }

        @Override
        public boolean signal(String name, Object... args) {
            computer.queueEvent(name, args);
            return true;
        }

        @Override
        public boolean pause(double seconds) {
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
        public void consumeCallBudget(double callCost) {
        }
    }
}
