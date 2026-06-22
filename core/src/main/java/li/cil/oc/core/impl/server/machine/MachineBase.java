package li.cil.oc.core.impl.server.machine;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.traits.Computer;
import li.cil.oc.core.impl.server.fs.FileSystem;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.impl.util.SaveHandlerDelegate;
import li.cil.oc.core.impl.util.ThreadPoolFactory;
import li.cil.oc.core.server.machine.CallbackWrapper;
import li.cil.oc.core.server.machine.Callbacks;
import li.cil.oc.core.server.machine.ProgramLocations;
import li.cil.oc.core.util.ConverterRegistry;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class MachineBase extends li.cil.oc.api.prefab.ManagedEnvironment implements li.cil.oc.api.machine.Machine, Runnable, DeviceInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineBase.class);
    private static final ScheduledExecutorService threadPool = ThreadPoolFactory.create("Computer", Settings.get().threads);
    private static final Set<Class<? extends Architecture>> checked = new LinkedHashSet<>();
    public final MachineHost host;
    public final Node node;
    public final li.cil.oc.api.network.ManagedEnvironment tmp;
    private final Deque<State> state = new ArrayDeque<>();
    private final Map<String, String> _components = new LinkedHashMap<>();
    private final Set<Component> addedComponents = new LinkedHashSet<>();
    private final Set<String> _users = new LinkedHashSet<>();
    private final Queue<Signal> signals = new LinkedList<>();
    private final int maxSignalQueueSize;
    public Architecture architecture;
    public int maxComponents = 0;
    public long worldTime = 0;

    @Override
    public long worldTime() {
        return worldTime;
    }

    @Override
    public int maxComponents() {
        return maxComponents;
    }

    @Override
    public Architecture architecture() {
        return architecture;
    }

    @Override
    public MachineHost host() {
        return host;
    }

    private volatile double maxCallBudget = 1.0;
    private boolean hasMemory = false;
    private volatile double callBudget = 0.0;
    private boolean inSynchronizedCall = false;
    private long uptime = 0;
    private long cpuTotal = 0;
    private long cpuStart = 0;
    private int remainIdle = 0;
    private int remainingPause = 0;
    private boolean usersChanged = false;
    private String message = null;
    private double cost;

    protected abstract void platformScheduleClose();
    protected abstract void platformUnscheduleClose();
    protected abstract void platformBeep(int frequency, int duration) ;
    protected abstract void platformBeep(String pattern) ;
    protected abstract void platformSendComputerUserList(String[] list) ;
    protected abstract boolean platformIsGamePaused();

    public MachineBase(MachineHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network)
                .withComponent("computer", Visibility.Neighbors)
                .withConnector(Settings.get().bufferComputer).create();
        this.tmp = Settings.get().tmpSize > 0 ?
                FileSystem.INSTANCE.asManagedEnvironment(
                        FileSystem.INSTANCE.fromMemory(Settings.get().tmpSize * 1024L), "tmpfs", null, null, 5) :
                null;
        this.cost = Settings.get().computerCost * Settings.get().tickFrequency;
        this.maxSignalQueueSize = Settings.get().maxSignalQueueSize;
        state.push(MachineBase.State.Stopped);
    }

    @Override
    public Node node() {
        return this.node;
    }

    public static void add(Class<? extends Architecture> architecture) {
        if (!checked.contains(architecture)) {
            try {
                architecture.getConstructor(li.cil.oc.api.machine.Machine.class);
            } catch (Throwable t) {
                throw new IllegalArgumentException("Architecture needs proper constructor", t);
            }
            checked.add(architecture);
        }
    }

    public static List<Class<? extends Architecture>> architectures() {
        return new ArrayList<>(checked);
    }

    public static String getArchitectureName(Class<? extends Architecture> architecture) {
        Architecture.Name a = architecture.getAnnotation(Architecture.Name.class);
        return a != null ? a.value() : architecture.getSimpleName();
    }

    @Override
    public void onHostChanged() {
        Iterable<ItemStack> components = host.internalComponents();
        maxComponents = 0;
        for (ItemStack item : components) {
            if (item != null) {
                Object driver = li.cil.oc.api.API.driver.driverFor(item, host.getClass());
                if (driver instanceof Processor) maxComponents += ((Processor) driver).supportedComponents(item);
            }
        }
        double sum = 0;
        int count = 0;
        for (ItemStack stack : components) {
            if (stack != null) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack, host.getClass());
                if (driver instanceof CallBudget) {
                    sum += ((CallBudget) driver).getCallBudget(stack);
                    count++;
                }
            }
        }
        maxCallBudget = count > 0 ? sum / count : 1.0;
        Architecture newArch = null;
        for (ItemStack stack : components) {
            if (stack != null) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack, host.getClass());
                if (driver instanceof Processor && ((Processor) driver).slot(stack).equals(Slot.CPU)) {
                    Class<? extends Architecture> clazz = ((Processor) driver).architecture(stack);
                    if (clazz != null) {
                        if (architecture == null || !architecture.getClass().equals(clazz)) {
                            try {
                                newArch = clazz.getConstructor(li.cil.oc.api.machine.Machine.class).newInstance(this);
                            } catch (Throwable t) {
                                Log.get().warn("Failed instantiating a CPU architecture.", t);
                            }
                        } else newArch = architecture;
                    }
                    break;
                }
            }
        }
        if (newArch != architecture) synchronized (this) {
            architecture = newArch;
            if (architecture != null && node != null && node.network() != null) architecture.onConnect();
        }
        hasMemory = architecture != null && architecture.recomputeMemory(components);
    }

    @Override
    public Map<String, String> components() {
        return new LinkedHashMap<>(_components);
    }

    @Override
    public String tmpAddress() {
        return tmp != null ? tmp.node().address() : null;
    }

    @Override
    public String lastError() {
        return message;
    }

    @Override
    public double getCostPerTick() {
        return cost / Settings.get().tickFrequency;
    }

    @Override
    public void setCostPerTick(double value) {
        cost = value * Settings.get().tickFrequency;
    }

    @Override
    public String[] users() {
        synchronized (_users) {
            return _users.toArray(new String[0]);
        }
    }

    @Override
    public double upTime() {
        if (uptime < 0) uptime = worldTime + uptime;
        return uptime / 20.0;
    }

    @Override
    public double cpuTime() {
        return (cpuTotal + (System.nanoTime() - cpuStart)) * 10e-10;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return host instanceof DeviceInfo ? ((DeviceInfo) host).getDeviceInfo() : null;
    }

    @Override
    public boolean canInteract(String player) {
        if (!Settings.get().canComputersBeOwned) return true;
        synchronized (_users) {
            if (_users.isEmpty() || _users.contains(player)) return true;
        }
        var server = host.level().getServer();
        if (server == null) return false;
        if (server.isSingleplayer()) return true;
        var profile = server.getPlayerList().getPlayerByName(player);
        return profile != null && server.getPlayerList().isOp(profile.getGameProfile());
    }

    @Override
    public boolean isRunning() {
        synchronized (state) {
            State t = state.peek();
            return t != State.Stopped && t != State.Stopping;
        }
    }

    @Override
    public boolean isPaused() {
        synchronized (state) {
            return state.peek() == State.Paused && remainingPause > 0;
        }
    }

    @Override
    public boolean start() {
        synchronized (state) {
            State t = state.peek();
            if (t == State.Stopped && node.network() != null) {
                onHostChanged();
                processAddedComponents();
                verifyComponents();
                if (!Settings.get().ignorePower && ((Connector) node).globalBuffer() < cost) {
                    crash("gui.opencomputers.error.noenergy");
                    return false;
                }
                if (architecture == null || maxComponents == 0) {
                    beep("-");
                    crash("gui.opencomputers.error.nocpu");
                    return false;
                }
                if (componentCount() > maxComponents) {
                    beep("-..");
                    crash("gui.opencomputers.error.componentoverflow");
                    return false;
                }
                if (!hasMemory) {
                    beep("-.");
                    crash("gui.opencomputers.error.noram");
                    return false;
                }
                if (!init()) {
                    beep("--");
                    return false;
                }
                switchTo(State.Starting);
                host.markChanged();
                uptime = 0;
                node.sendToReachable("computer.started");
                return true;
            } else if (t == State.Paused && remainingPause > 0) {
                remainingPause = 0;
                host.markChanged();
                return true;
            } else if (t == State.Stopping) {
                switchTo(State.Restarting);
                host.markChanged();
                platformUnscheduleClose();
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean pause(double seconds) {
        int ticksToPause = Math.max((int) (seconds * 20), 0);
        synchronized (state) {
            State t = state.peek();
            if (t == State.Stopping || t == State.Stopped) return false;
            if (t == State.Paused && ticksToPause <= remainingPause) return false;
        }
        synchronized (this) {
            synchronized (state) {
                State t = state.peek();
                if (t == State.Stopping || t == State.Stopped) return false;
                if (t == State.Paused && ticksToPause <= remainingPause) return false;
                if (t != State.Paused) state.push(State.Paused);
                remainingPause = ticksToPause;
                host.markChanged();
                return true;
            }
        }
    }

    @Override
    public boolean stop() {
        synchronized (state) {
            State t = state.peek();
            if (t == State.Stopped || t == State.Stopping) return false;
            state.push(State.Stopping);
            platformScheduleClose();
            return true;
        }
    }

    @Override
    public void consumeCallBudget(double callCost) {
        if (architecture != null && architecture.isInitialized() && !inSynchronizedCall) {
            double c = Math.max(0.0, callCost);
            synchronized (this) {
                if (c > callBudget) throw new LimitReachedException();
                callBudget -= c;
            }
        }
    }

    @Override
    public void beep(short frequency, short duration) {
        platformBeep(frequency, duration);
    }

    @Override
    public void beep(String pattern) {
        platformBeep(pattern);
    }

    @Override
    public void crash(String message) {
        this.message = message;
        synchronized (state) {
            stop();
            if (state.peek() == State.Stopping) {
                state.clear();
                state.push(State.Stopping);
            }
        }
    }

    @Override
    public boolean signal(String name, Object... args) {
        synchronized (state) {
            State t = state.peek();
            if (t == State.Stopped || t == State.Stopping) return false;
        }
        synchronized (signals) {
            if (signals.size() >= maxSignalQueueSize) return false;
            Object[] sanitized = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                sanitized[i] = convertArg(args[i]);
            }
            signals.add(new Signal(name, sanitized));
        }
        if (architecture != null) architecture.onSignal();
        return true;
    }

    private Object convertArg(Object arg) {
        return switch (arg) {
            case Boolean ignored -> arg;
            case Byte ignored -> arg;
            case Short ignored -> arg;
            case Integer ignored -> arg;
            case Long ignored -> arg;
            case Character c -> (int) c;
            case Float aFloat -> aFloat.doubleValue();
            case Double ignored -> arg;
            case String ignored -> arg;
            case byte[] ignored -> arg;
            case Tag ignored -> arg;
            case Map<?, ?> map -> {
                Map<Object, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    Object k = convertArg(e.getKey());
                    Object v = convertArg(e.getValue());
                    if (k != null && v != null) converted.put(k, v);
                }
                yield converted;
            }
            case null, default -> null;
        };
    }

    @Override
    public li.cil.oc.api.machine.Signal popSignal() {
        synchronized (signals) {
            Signal s = signals.poll();
            return s != null ? new li.cil.oc.api.machine.Signal() {
                public String name() {
                    return s.name;
                }

                public Object[] args() {
                    return ConverterRegistry.get().convert(s.args);
                }
            } : null;
        }
    }

    @Override
    public Map<String, Callback> methods(Object value) {
        Map<String, CallbackWrapper> cbs = Callbacks.apply(value);
        Map<String, Callback> r = new LinkedHashMap<>();
        for (Map.Entry<String, CallbackWrapper> e : cbs.entrySet())
            r.put(e.getKey(), e.getValue().annotation());
        return r;
    }

    @Override
    public Object[] invoke(String address, String method, Object[] args) {
        if (node == null || node.network() == null) throw new LimitReachedException();
        Node targetNode = node.network().node(address);
        if (!(targetNode instanceof Component c)) throw new IllegalArgumentException("no such component");
        if (!c.canBeSeenFrom(node) && c != node) throw new IllegalArgumentException("no such component");
        Callback a = c.annotation(method);
        if (a.direct()) consumeCallBudget(1.0 / a.limit());
        return c.invoke(method, this, args);
    }

    @Override
    public Object[] invoke(Value value, String method, Object[] args) {
        Map<String, CallbackWrapper> cbs = Callbacks.apply(value);
        CallbackWrapper cb = cbs.get(method);
        if (cb == null) throw new RuntimeException(new NoSuchMethodException(method));
        if (cb.annotation().direct()) consumeCallBudget(1.0 / cb.annotation().limit());
        return ConverterRegistry.get().convert(cb.apply(value, this, new ArgumentsImpl(args)));
    }

    @Override
    public void addUser(String name) {
        if (_users.size() >= Settings.get().maxUsers) throw new RuntimeException("too many users");
        if (_users.contains(name)) throw new RuntimeException("user exists");
        if (name.length() > Settings.get().maxUsernameLength) throw new RuntimeException("username too long");
        var server = host.level().getServer();
        if (server == null || !java.util.Arrays.asList(server.getPlayerNames()).contains(name))
            throw new RuntimeException("player must be online");
        synchronized (_users) {
            _users.add(name);
            usersChanged = true;
        }
    }

    @Override
    public boolean removeUser(String name) {
        synchronized (_users) {
            boolean s = _users.remove(name);
            if (s) usersChanged = true;
            return s;
        }
    }

    @Callback(doc = "function():boolean -- Starts the computer.")
    public Object[] start(Context context, Arguments args) {
        return ResultWrapper.result(!isPaused() && start());
    }

    @Callback(doc = "function():boolean -- Stops the computer.")
    public Object[] stop(Context context, Arguments args) {
        return ResultWrapper.result(stop());
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the computer is running.")
    public Object[] isRunning(Context context, Arguments args) {
        return ResultWrapper.result(isRunning());
    }

    @Callback(doc = "function([frequency:string or number[, duration:number]]) -- Plays a tone.")
    @SuppressWarnings("SameReturnValue")
    public Object[] beep(Context context, Arguments args) {
        if (args.count() == 1 && args.isString(0)) {
            beep(args.checkString(0));
            return null;
        }
        int freq = args.optInteger(0, 440);
        if (freq < 20 || freq > 2000) throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]");
        double dur = args.optDouble(1, 0.1);
        int ms = Math.clamp((int) (dur * 1000), 50, 5000);
        context.pause(ms / 1000.0);
        beep((short) freq, (short) ms);
        return null;
    }

    @Callback(doc = "function():table -- Collect information on all connected devices.")
    public Object[] getDeviceInfo(Context context, Arguments args) {
        context.pause(1);
        Map<Object, Object> r = new LinkedHashMap<>();
        for (Node n : node.network().nodes()) {
            if (n.host() instanceof DeviceInfo) {
                if (n instanceof Component && (((Component) n).canBeSeenFrom(node) || n == node) || n.canBeReachedFrom(node)) {
                    Map<String, String> info = ((DeviceInfo) n.host()).getDeviceInfo();
                    if (info != null) r.put(n.address(), info);
                }
            }
        }
        return ResultWrapper.result(r);
    }

    @Callback(doc = "function():table -- Returns a map of program name to disk label.")
    public Object[] getProgramLocations(Context context, Arguments args) {
        return ResultWrapper.result(ProgramLocations.getMappings(getArchitectureName(architecture.getClass())));
    }

    public boolean isExecuting() {
        synchronized (state) {
            return state.contains(State.Running);
        }
    }

    public java.util.Deque<State> state() {
        synchronized (state) {
            return new ArrayDeque<>(state);
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        synchronized (state) {
            if (state.peek() == State.Stopped) {
                return;
            }
        }
        processAddedComponents();
        if (maxComponents > 0 && componentCount() > maxComponents) {
            beep("-..");
            crash("gui.opencomputers.error.componentoverflow");
        }
        worldTime = host.level().getGameTime();
        uptime++;
        if (remainIdle > 0) remainIdle--;
        callBudget = maxCallBudget;
        if (host.level().getGameTime() % Settings.get().tickFrequency == 0) {
            synchronized (state) {
                State t = state.peek();
                if (t != State.Paused && t != State.Restarting && t != State.Stopping && t != State.Stopped) {
                    if (t == State.Sleeping && remainIdle > 0 && signals.isEmpty()) {
                        if (!((Connector) node).tryChangeBuffer(-cost * Settings.get().sleepCostFactor))
                            crash("gui.opencomputers.error.noenergy");
                    } else {
                        if (!((Connector) node).tryChangeBuffer(-cost)) crash("gui.opencomputers.error.noenergy");
                    }
                }
            }
        }
        if (host.level().getGameTime() % 20 == 0 && usersChanged) {
            String[] list;
            synchronized (_users) {
                usersChanged = false;
                list = users();
            }
            if (host instanceof Computer) platformSendComputerUserList(list);
        }
        State current;
        synchronized (state) {
            current = state.peek();
        }
        if (current == null) return;
        switch (current.name) {
            case "Starting":
                verifyComponents();
                switchTo(State.Yielded);
                break;
            case "Restarting":
                close();
                if (Settings.get().eraseTmpOnReboot) {
                    if (tmp != null) tmp.node().remove();
                    if (tmp != null) node.connect(tmp.node());
                }
                node.sendToReachable("computer.stopped");
                start();
                break;
            case "Sleeping":
                if (remainIdle <= 0 || !signals.isEmpty()) {
                    switchTo(State.Yielded);
                }
                break;
            case "Paused":
                if (remainingPause > 0) remainingPause--;
                else {
                    verifyComponents();
                    state.pop();
                    switchTo(state.peek());
                }
                break;
            case "SynchronizedCall":
                switchTo(State.Running);
                try {
                    inSynchronizedCall = true;
                    architecture.runSynchronized();
                    inSynchronizedCall = false;
                    synchronized (state) {
                        State t = state.peek();
                        if (t == State.Running) switchTo(State.SynchronizedReturn);
                        else if (t == State.Paused) {
                            state.pop();
                            state.pop();
                            state.push(State.SynchronizedReturn);
                            state.push(State.Paused);
                        } else if (t == State.Stopping) {
                            state.clear();
                            state.push(State.Stopping);
                        }
                    }
                    host.markChanged();
                } catch (Throwable e) {
                    if ("not enough memory".equals(e.getMessage())) crash("gui.opencomputers.error.outofmemory");
                    else {
                        LOGGER.warn("Faulty architecture for synchronized calls.", e);
                        crash("gui.opencomputers.error.internalerror");
                    }
                } finally {
                    inSynchronizedCall = false;
                }
                break;
        }
        boolean shouldClose;
        synchronized (state) {
            shouldClose = state.peek() == State.Stopping;
        }
        if (shouldClose) {
            synchronized (this) {
                synchronized (state) {
                    tryClose();
                }
            }
        }
    }

    @Override
    public void onMessage(Message msg) {
        if ("computer.signal".equals(msg.name()) && msg.data().length >= 1 && msg.data()[0] instanceof String) {
            Object[] a = new Object[msg.data().length];
            a[0] = msg.source().address();
            System.arraycopy(msg.data(), 1, a, 1, msg.data().length - 1);
            signal((String) msg.data()[0], a);
        } else if ("computer.checked_signal".equals(msg.name()) && msg.data().length >= 2 && msg.data()[0] instanceof Player && msg.data()[1] instanceof String && canInteract(((Player) msg.data()[0]).getScoreboardName())) {
            Object[] a = new Object[msg.data().length - 1];
            a[0] = msg.source().address();
            System.arraycopy(msg.data(), 2, a, 1, msg.data().length - 2);
            signal((String) msg.data()[1], a);
        } else {
            if ("computer.start".equals(msg.name()) && !isPaused()) start();
            else if ("computer.stop".equals(msg.name())) stop();
        }
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            _components.put(this.node.address(), this.node instanceof Component ? ((Component) this.node).name() : this.node.address());
            if (tmp != null) node.connect(tmp.node());
            if (architecture != null) architecture.onConnect();
        } else if (node instanceof Component) addComponent((Component) node);
        host.onMachineConnect(node);
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node) {
            close();
            if (tmp != null) tmp.node().remove();
        } else if (node instanceof Component) removeComponent((Component) node);
        host.onMachineDisconnect(node);
    }

    public void addComponent(Component c) {
        if (!_components.containsKey(c.address())) addedComponents.add(c);
    }

    public void removeComponent(Component c) {
        synchronized (_components) {
            if (_components.containsKey(c.address())) {
                _components.remove(c.address());
                signal("component_removed", c.address(), c.name());
            }
        }
        addedComponents.remove(c);
    }

    private void processAddedComponents() {
        if (!addedComponents.isEmpty()) {
            for (Component c : addedComponents) {
                if (c.canBeSeenFrom(node)) {
                    synchronized (_components) {
                        _components.put(c.address(), c.name());
                    }
                    if (architecture != null && architecture.isInitialized())
                        signal("component_added", c.address(), c.name());
                }
            }
            addedComponents.clear();
        }
    }

    private void verifyComponents() {
        Set<String> invalid = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : _components.entrySet()) {
            String name = e.getValue();
            Node n = node.network().node(e.getKey());
            if (!(n instanceof Component) || !((Component) n).name().equals(name)) {
                if ("filesystem".equals(name)) {
                    LOGGER.trace("A component of type '{}' disappeared ({}). This usually means that it didn't save its node.", name, e.getKey());
                    LOGGER.trace("If this was a file system provided by a ComputerCraft peripheral, this is normal.");
                } else {
                    LOGGER.warn("A component of type '{}' disappeared ({}). This usually means that it didn't save its node.", name, e.getKey());
                }
                signal("component_removed", e.getKey(), name);
                invalid.add(e.getKey());
            }
        }
        for (String a : invalid) _components.remove(a);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (this) {
            synchronized (state) {
                close();
                state.clear();
                super.load(nbt, provider);
                int[] stateIds = nbt.getIntArray("state");
                for (int i = stateIds.length - 1; i >= 0; i--) {
                    state.push(State.fromId(stateIds[i]));
                }
                ListTag ul = nbt.getList("users", Tag.TAG_STRING);
                for (int i = 0; i < ul.size(); i++) _users.add(ul.getString(i));
                if (nbt.contains("message")) message = nbt.getString("message");
                _components.clear();
                ListTag cl = nbt.getList("components", Tag.TAG_COMPOUND);
                for (int i = 0; i < cl.size(); i++) {
                    CompoundTag t = cl.getCompound(i);
                    _components.put(t.getString("address"), t.getString("name"));
                }
                if (tmp != null) {
                    if (nbt.contains("tmp")) {
                        tmp.load(nbt.getCompound("tmp"), provider);
                    } else {
                        tmp.load(SaveHandlerDelegate.get().loadNBT(nbt, node.address() + "_tmp"), provider);
                    }
                }
                if (!state.isEmpty() && isRunning() && init()) {
                    try {
                        architecture.load(nbt);
                        ListTag sl = nbt.getList("signals", Tag.TAG_COMPOUND);
                        for (int i = 0; i < sl.size(); i++) {
                            CompoundTag sn = sl.getCompound(i);
                            CompoundTag an = sn.getCompound("args");
                            int len = an.getInt("length");
                            Object[] sa = new Object[len];
                            for (int j = 0; j < len; j++) {
                                String key = "arg" + j;
                                Tag tag = an.get(key);
                                switch (tag) {
                                    case ByteTag byteTag -> {
                                        byte b = byteTag.getAsByte();
                                        sa[j] = b == -1 ? null : b == 1;
                                    }
                                    case LongTag longTag -> sa[j] = longTag.getAsLong();
                                    case DoubleTag doubleTag -> sa[j] = doubleTag.getAsDouble();
                                    case StringTag stringTag -> sa[j] = stringTag.getAsString();
                                    case ByteArrayTag byteTags -> sa[j] = byteTags.getAsByteArray();
                                    case ListTag list -> {
                                        Map<String, String> d = new LinkedHashMap<>();
                                        for (int k = 0; k < list.size(); k += 2)
                                            d.put(list.getString(k), list.getString(k + 1));
                                        sa[j] = d;
                                    }
                                    case CompoundTag ignored -> sa[j] = tag;
                                    case null, default -> sa[j] = null;
                                }
                            }
                            signals.add(new Signal(sn.getString("name"), sa));
                        }
                        uptime = nbt.getLong("uptime");
                        cpuTotal = nbt.getLong("cpuTime");
                        remainingPause = nbt.getInt("remainingPause");
                        if (state.peek() != State.Restarting) pause(Settings.get().startupDelay);
                    } catch (Throwable t) {
                        LOGGER.error("Error loading computer state", t);
                        close();
                    }
                } else {
                    onHostChanged();
                    close();
                }
            }
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (this) {
            synchronized (state) {
                if (isExecuting() || SaveHandlerDelegate.get().savingForClients()) return;
                pause(0.05);
                super.save(nbt, provider);
                processAddedComponents();
                int[] sa = new int[state.size()];
                int idx = 0;
                for (State s : state) sa[idx++] = s.id;
                nbt.putIntArray("state", sa);
                ListTag ul = new ListTag();
                for (String u : _users) ul.add(net.minecraft.nbt.StringTag.valueOf(u));
                nbt.put("users", ul);
                if (message != null) nbt.putString("message", message);
                ListTag cl = new ListTag();
                for (Map.Entry<String, String> e : _components.entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("address", e.getKey());
                    t.putString("name", e.getValue());
                    cl.add(t);
                }
                nbt.put("components", cl);
                if (tmp != null) saveTmp(nbt, tmp);
                if (state.peek() != State.Stopped) try {
                    architecture.save(nbt);
                    ListTag sl = new ListTag();
                    for (Signal s : signals) {
                        CompoundTag sn = new CompoundTag();
                        sn.putString("name", s.name);
                        CompoundTag an = new CompoundTag();
                        an.putInt("length", s.args.length);
                        for (int i = 0; i < s.args.length; i++) {
                            String key = "arg" + i;
                            Object arg = s.args[i];
                            switch (arg) {
                                case Boolean b -> an.putByte(key, (byte) (b ? 1 : 0));
                                case Long l -> an.putLong(key, l);
                                case Double v -> an.putDouble(key, v);
                                case String string -> an.putString(key, string);
                                case byte[] bytes -> an.putByteArray(key, bytes);
                                case Map<?, ?> map -> {
                                    ListTag list = new ListTag();
                                    for (Map.Entry<?, ?> e : map.entrySet()) {
                                        list.add(net.minecraft.nbt.StringTag.valueOf(String.valueOf(e.getKey())));
                                        list.add(net.minecraft.nbt.StringTag.valueOf(String.valueOf(e.getValue())));
                                    }
                                    an.put(key, list);
                                }
                                case CompoundTag compoundTag -> an.put(key, compoundTag);
                                case null, default -> an.putByte(key, (byte) -1);
                            }
                        }
                        sn.put("args", an);
                        sl.add(sn);
                    }
                    nbt.put("signals", sl);
                    nbt.putLong("uptime", uptime);
                    nbt.putLong("cpuTime", cpuTotal);
                    nbt.putInt("remainingPause", remainingPause);
                } catch (Throwable t) {
                    LOGGER.error("Error saving computer state", t);
                }
            }
        }
    }

    private boolean init() {
        onHostChanged();
        if (architecture == null) return false;
        message = null;
        signals.clear();
        if (node.network() != null && tmp != null) node.connect(tmp.node());
        try {
            return architecture.initialize();
        } catch (Throwable ex) {
            LOGGER.warn("Failed initializing computer.", ex);
            close();
            return false;
        }
    }

    public boolean tryClose() {
        if (isExecuting()) return false;
        close();
        if (tmp != null) tmp.node().remove();
        if (node.network() != null && tmp != null) node.connect(tmp.node());
        node.sendToReachable("computer.stopped");
        return true;
    }

    private void close() {
        synchronized (state) {
            if (!state.isEmpty() && state.peek() == State.Stopped) return;
        }
        synchronized (this) {
            synchronized (state) {
                state.clear();
                state.push(State.Stopped);
                if (architecture != null) architecture.close();
                signals.clear();
                uptime = 0;
                cpuTotal = 0;
                cpuStart = 0;
                remainIdle = 0;
            }
        }
        host.markChanged();
    }

    private void saveTmp(CompoundTag nbt, li.cil.oc.api.network.ManagedEnvironment fs) {
        if (node() == null) return;
        CompoundTag tmpTag = new CompoundTag();
        fs.save(tmpTag, host.level().registryAccess());
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NbtIo.write(tmpTag, dos);
            SaveHandlerDelegate.get().scheduleSave(host, nbt, node().address() + "_tmp", baos.toByteArray());
        } catch (java.io.IOException e) {
            LOGGER.warn("Error saving tmp data.", e);
        }
    }

    private State switchTo(State value) {
        State prev;
        synchronized (state) {
            prev = state.pop();
            if (value == State.Stopping || value == State.Restarting) state.clear();
            state.push(value);
            if (value == State.Yielded || value == State.SynchronizedReturn) {
                remainIdle = 0;
                threadPool.schedule(this, Settings.get().executionDelay, TimeUnit.MILLISECONDS);
            }
        }
        host.markChanged();
        return prev;
    }

    private boolean isGamePaused() {
        return platformIsGamePaused();
    }

    @Override
    public void run() {
        synchronized (this) {
            boolean isSyncReturn;
            synchronized (state) {
                State t = state.peek();
                if (t != State.Yielded && t != State.SynchronizedReturn) {
                    return;
                }
                if (isGamePaused()) {
                    state.push(State.Paused);
                    return;
                }
                isSyncReturn = switchTo(State.Running) == State.SynchronizedReturn;
            }
            cpuStart = System.nanoTime();
            try {
                ExecutionResult result = architecture.runThreaded(isSyncReturn);
                synchronized (state) {
                    State t = state.peek();
                    if (t == State.Running) {
                        if (result instanceof ExecutionResult.Sleep) {
                            synchronized (signals) {
                                if (signals.isEmpty() && ((ExecutionResult.Sleep) result).ticks > 0) {
                                    switchTo(State.Sleeping);
                                    remainIdle = ((ExecutionResult.Sleep) result).ticks;
                                } else switchTo(State.Yielded);
                            }
                        } else if (result instanceof ExecutionResult.SynchronizedCall) switchTo(State.SynchronizedCall);
                        else if (result instanceof ExecutionResult.Shutdown) {
                            if (((ExecutionResult.Shutdown) result).reboot) switchTo(State.Restarting);
                            else switchTo(State.Stopping);
                        } else if (result instanceof ExecutionResult.Error) {
                            beep("--");
                            crash(((ExecutionResult.Error) result).message != null ? ((ExecutionResult.Error) result).message : "unknown error");
                        }
                    } else if (t == State.Paused) {
                        state.pop();
                        state.pop();
                        if (result instanceof ExecutionResult.Sleep) {
                            remainIdle = ((ExecutionResult.Sleep) result).ticks;
                            state.push(State.Sleeping);
                        } else if (result instanceof ExecutionResult.SynchronizedCall)
                            state.push(State.SynchronizedCall);
                        else if (result instanceof ExecutionResult.Shutdown) {
                            if (((ExecutionResult.Shutdown) result).reboot) state.push(State.Restarting);
                            else state.push(State.Stopping);
                        } else if (result instanceof ExecutionResult.Error)
                            crash(((ExecutionResult.Error) result).message != null ? ((ExecutionResult.Error) result).message : "unknown error");
                        state.push(State.Paused);
                    } else if (t == State.Stopping) {
                        state.clear();
                        state.push(State.Stopping);
                    }
                }
            } catch (Throwable e) {
                LOGGER.warn("runThreaded threw error", e);
                crash("gui.opencomputers.error.internalerror");
            }
            cpuTotal += System.nanoTime() - cpuStart;
        }
        host.markChanged();
    }

    public int componentCount() {
        double c = -1.0;
        for (String name : _components.values()) c += "filesystem".equals(name) ? 0.25 : 1.0;
        for (Component co : addedComponents) c += "filesystem".equals(co.name()) ? 0.25 : 1.0;
        return (int) c;
    }

    public static class State {
        public static final State Stopped = new State(0, "Stopped");
        public static final State Starting = new State(1, "Starting");
        public static final State Restarting = new State(2, "Restarting");
        public static final State Stopping = new State(3, "Stopping");
        public static final State Paused = new State(4, "Paused");
        public static final State SynchronizedCall = new State(5, "SynchronizedCall");
        public static final State SynchronizedReturn = new State(6, "SynchronizedReturn");
        public static final State Yielded = new State(7, "Yielded");
        public static final State Sleeping = new State(8, "Sleeping");
        public static final State Running = new State(9, "Running");
        private static final State[] VALUES = {Stopped, Starting, Restarting, Stopping, Paused, SynchronizedCall, SynchronizedReturn, Yielded, Sleeping, Running};
        public final int id;
        public final String name;

        private State(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public static State fromId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : Stopped;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public record Signal(String name, Object[] args) {

    }
}
