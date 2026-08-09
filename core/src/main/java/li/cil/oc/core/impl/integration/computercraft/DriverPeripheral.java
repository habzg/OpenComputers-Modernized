package li.cil.oc.core.impl.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.core.impl.server.driver.Registry;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.BlacklistedPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public final class DriverPeripheral implements DriverBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverPeripheral.class);

    private static Set<Class<?>> blacklist;

    private static final List<PeripheralFinder> finders = new ArrayList<>();

    @FunctionalInterface
    public interface PeripheralFinder {
        IPeripheral find(Level world, BlockPos pos, Direction side);
    }

    public static void addPeripheralFinder(final PeripheralFinder finder) {
        finders.add(finder);
    }

    private boolean isAllowed(final Object o) {
        // Check for our interface first, as that has priority.
        if (o instanceof BlacklistedPeripheral) {
            return !((BlacklistedPeripheral) o).isPeripheralBlacklisted();
        }

        // Delayed initialization of the resolved classes to allow registering
        // additional entries via IMC.
        if (blacklist == null) {
            blacklist = new HashSet<>();
            for (String name : OCSettings.get().peripheralBlacklist) {
                try {
                    blacklist.add(Class.forName(name));
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        for (Class<?> clazz : blacklist) {
            if (clazz.isInstance(o)) return false;
        }
        return true;
    }

    private IDynamicPeripheral findPeripheral(final Level world, final BlockPos pos, final Direction side) {
        for (PeripheralFinder finder : finders) {
            try {
                final IPeripheral peripheral = finder.find(world, pos, side);
                if (peripheral != null) {
                    if (!isAllowed(peripheral)) {
                        return null;
                    }
                    if (peripheral instanceof IDynamicPeripheral dynamic) {
                        return dynamic;
                    }
                    return new AnnotationPeripheral(peripheral);
                }
            } catch (Exception e) {
                LOGGER.warn("Error accessing ComputerCraft peripheral @ ({}, {}, {}).", pos.getX(), pos.getY(), pos.getZ(), e);
            }
        }
        return null;
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null
                // This ensures we don't get duplicate components, in case the
                // block entity is natively compatible with OpenComputers.
                && !li.cil.oc.api.network.Environment.class.isAssignableFrom(blockEntity.getClass())
                // Same for blocks with a dedicated (non-generic) driver, e.g.
                // via a managed environment wrapper.
                && !hasSpecificDriver(world, pos, side)
                // The black list is used to avoid peripherals that are known
                // to be incompatible with OpenComputers when used directly.
                && isAllowed(blockEntity)
                // Actual check if it's a peripheral.
                && findPeripheral(world, pos, side) != null;
    }

    private static boolean hasSpecificDriver(final Level world, final BlockPos pos, final Direction side) {
        for (DriverBlock driver : Registry.INSTANCE.blockDrivers()) {
            if (driver != null && !(driver instanceof DriverPeripheral) && !driver.isGeneric() && driver.worksWith(world, pos, side)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        final IDynamicPeripheral peripheral = findPeripheral(world, pos, side);
        return peripheral == null ? null : new Environment(peripheral);
    }

    @Override
    public boolean isGeneric() {
        return true;
    }

    public static class Environment extends li.cil.oc.api.prefab.AbstractManagedEnvironment
            implements li.cil.oc.api.network.ManagedPeripheral, NamedBlock {
        protected final IDynamicPeripheral peripheral;

        protected final CallableHelper helper;

        protected final Map<String, FakeComputerAccess> accesses = new HashMap<>();

        public Environment(final IDynamicPeripheral peripheral) {
            this.peripheral = peripheral;
            helper = new CallableHelper(peripheral.getMethodNames());
            setNode(Network.newNode(this, Visibility.Network).create());
        }

        @Override
        public String[] methods() {
            return peripheral.getMethodNames();
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Arguments args) {
            final int index = helper.methodIndex(method);
            final Object[] argArray = helper.convertArguments(args);
            final FakeComputerAccess access;
            if (accesses.containsKey(context.node().address())) {
                access = accesses.get(context.node().address());
            } else {
                access = new FakeComputerAccess(this, context);
            }
            try {
                var luaContext = new UnsupportedLuaContext();
                var result = peripheral.callMethod(access, luaContext, index, new IArguments() {
                    @Override
                    public int count() {
                        return argArray.length;
                    }

                    @Override
                    public Object get(int idx) {
                        if (idx < 0 || idx >= argArray.length) return null;
                        return argArray[idx];
                    }

                    @Override
                    public @NotNull String getType(int idx) {
                        Object o = get(idx);
                        return switch (o) {
                            case null -> "nil";
                            case String ignored -> "string";
                            case Boolean ignored -> "boolean";
                            case Number ignored -> "number";
                            //noinspection rawtypes
                            case Map ignored -> "table";
                            default -> "userdata";
                        };
                    }

                    @Override
                    public @NotNull IArguments drop(int count) {
                        return this;
                    }
                });
                if (result.getCallback() == null) {
                    return result.getResult();
                }
                int maxIterations = 100;
                while (result.getCallback() != null && maxIterations-- > 0) {
                    Object[] yieldArgs = result.getResult();
                    String filter = (yieldArgs != null && yieldArgs.length > 0) ? String.valueOf(yieldArgs[0]) : null;
                    Object[] eventData = null;
                    if ("task_completed".equals(filter) || "task_complete".equals(filter)) {
                        eventData = luaContext.consumeTaskResult();
                    }
                    if (eventData == null) {
                        return new Object[]{null, "CC yield not supported via OC bridge: " + filter};
                    }
                    result = result.getCallback().resume(eventData);
                }
                return result.getResult();
            } catch (LuaException e) {
                return new Object[]{null, e.getMessage()};
            }
        }

        @Override
        public String preferredName() {
            return peripheral.getType();
        }

        @Override
        public int priority() {
            return -1;
        }

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);
            if (node.host() instanceof Context && !accesses.containsKey(node.address())) {
                final FakeComputerAccess access = new FakeComputerAccess(this, (Context) node.host());
                accesses.put(node.address(), access);
                peripheral.attach(access);
            }
        }

        @Override
        public void onDisconnect(final Node node) {
            super.onDisconnect(node);
            if (node.host() instanceof Context) {
                final FakeComputerAccess access = accesses.remove(node.address());
                if (access != null) {
                    peripheral.detach(access);
                }
            } else if (node == this.node()) {
                for (FakeComputerAccess access : accesses.values()) {
                    peripheral.detach(access);
                    access.close();
                }
                accesses.clear();
            }
        }

        public static class FakeComputerAccess implements IComputerAccess {
            protected final Environment owner;
            protected final Context context;
            protected final Map<String, ManagedEnvironment> fileSystems = new HashMap<>();

            public FakeComputerAccess(final Environment owner, final Context context) {
                this.owner = owner;
                this.context = context;
            }

            public void close() {
                for (ManagedEnvironment fileSystem : fileSystems.values()) {
                    fileSystem.node().remove();
                }
                fileSystems.clear();
            }

            @Override
            public String mount(final @NotNull String desiredLocation, final @NotNull Mount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount)));
            }

            @Override
            public String mount(@NotNull String desiredLocation, @NotNull Mount mount, @NotNull String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(
                        desiredLocation,
                        FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount), driveName));
            }

            @Override
            public String mountWritable(final @NotNull String desiredLocation, final @NotNull WritableMount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount)));
            }

            @Override
            public String mountWritable(@NotNull String desiredLocation, @NotNull WritableMount mount, @NotNull String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(
                        desiredLocation,
                        FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount), driveName));
            }

            private String mount(final String path, final ManagedEnvironment fileSystem) {
                fileSystems.put(path, fileSystem);
                context.node().connect(fileSystem.node());
                return path;
            }

            @Override
            public void unmount(final String location) {
                final ManagedEnvironment fileSystem = fileSystems.remove(location);
                if (fileSystem != null) {
                    fileSystem.node().remove();
                }
            }

            @Override
            public int getID() {
                return context.node().address().hashCode();
            }

            @Override
            public void queueEvent(final @NotNull String event, final Object @NotNull [] arguments) {
                context.signal(event, arguments);
            }

            @Override
            public @NotNull String getAttachmentName() {
                return owner.node().address();
            }

            @Override
            public @NotNull Map<String, IPeripheral> getAvailablePeripherals() {
                return Map.of(owner.node().address(), owner.peripheral);
            }

            @Override
            public IPeripheral getAvailablePeripheral(@NotNull String name) {
                return owner.node().address().equals(name) ? owner.peripheral : null;
            }

            @Override
            @SuppressWarnings("DataFlowIssue")
            public @NotNull WorkMonitor getMainThreadMonitor() {
                return null;
            }
        }

        public static final class UnsupportedLuaContext implements ILuaContext {
            private long nextTaskId = 0;
            private Object[] pendingTaskResult = null;

            @Override
            public long issueMainThreadTask(@NotNull LuaTask task) {
                final long taskId = ++nextTaskId;
                try {
                    final Object[] taskResult = task.execute();
                    final Object[] event = new Object[3 + (taskResult != null ? taskResult.length : 0)];
                    event[0] = "task_completed";
                    event[1] = taskId;
                    event[2] = true;
                    if (taskResult != null) {
                        System.arraycopy(taskResult, 0, event, 3, taskResult.length);
                    }
                    pendingTaskResult = event;
                } catch (LuaException e) {
                    pendingTaskResult = new Object[]{"task_completed", taskId, false, e.getMessage()};
                } catch (Exception e) {
                    pendingTaskResult = new Object[]{"task_completed", taskId, false,
                            e.getMessage() != null ? e.getMessage() : e.toString()};
                }
                return taskId;
            }

            @Override
            public @NotNull MethodResult executeMainThreadTask(@NotNull LuaTask task) throws LuaException {
                try {
                    return MethodResult.of(task.execute());
                } catch (LuaException e) {
                    throw e;
                } catch (Exception e) {
                    throw new LuaException(e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }

            Object[] consumeTaskResult() {
                final Object[] result = pendingTaskResult;
                pendingTaskResult = null;
                return result;
            }
        }
    }
}
