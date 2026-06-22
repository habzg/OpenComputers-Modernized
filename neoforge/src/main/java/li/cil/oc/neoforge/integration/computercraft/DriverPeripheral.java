package li.cil.oc.neoforge.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.api.peripheral.WorkMonitor;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.BlacklistedPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public final class DriverPeripheral implements li.cil.oc.api.driver.SidedBlock {
    private static Set<Class<?>> blacklist;

    private boolean isAllowed(final Object o) {
        if (o instanceof BlacklistedPeripheral) {
            return !((BlacklistedPeripheral) o).isPeripheralBlacklisted();
        }

        if (blacklist == null) {
            blacklist = new HashSet<>();
            for (String name : Settings.get().peripheralBlacklist) {
                try {
                    Class<?> clazz = Class.forName(name);
                    blacklist.add(clazz);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        for (Class<?> clazz : blacklist) {
            if (clazz.isInstance(o)) return false;
        }
        return true;
    }

    private IDynamicPeripheral findPeripheral(
            final Level world, final int x, final int y, final int z, final Direction side) {
        try {
            BlockPos pos = new BlockPos(x, y, z);
            var cap = world.getCapability(PeripheralCapability.get(), pos, side);
            if (cap instanceof IDynamicPeripheral p && isAllowed(p)) {
                return p;
            }
            if (cap instanceof IPeripheral p && isAllowed(p)) {
                return new AnnotationPeripheral(p);
            }
        } catch (Exception e) {
            OpenComputers.log()
                    .warn("Error accessing ComputerCraft peripheral @ ({}, {}, {}).", x, y, z, e);
        }
        return null;
    }

    @Override
    public boolean worksWith(final Level world, final int x, final int y, final int z, final Direction side) {
        final BlockEntity blockEntity = world.getBlockEntity(new BlockPos(x, y, z));
        return blockEntity != null
                && !li.cil.oc.api.network.Environment.class.isAssignableFrom(blockEntity.getClass())
                && isAllowed(blockEntity)
                && findPeripheral(world, x, y, z, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level world, final int x, final int y, final int z, final Direction side) {
        return new Environment(findPeripheral(world, x, y, z, side));
    }

    public static class Environment extends li.cil.oc.api.prefab.ManagedEnvironment
            implements li.cil.oc.api.network.ManagedPeripheral, NamedBlock {
        protected final IDynamicPeripheral peripheral;

        protected final CallableHelper helper;

        protected final Map<String, FakeComputerAccess> accesses = new HashMap<>();

        public Environment(final @UnknownNullability IDynamicPeripheral peripheral) {
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
                var result = peripheral.callMethod(access, UnsupportedLuaContext.instance(), index, new IArguments() {
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
                throw new UnsupportedOperationException("ComputerCraft yield not supported via OC bridge");
            } catch (LuaException e) {
                return new Object[]{null, e.getMessage()};
            }
        }

        @Override
        public String preferredName() {
            return "cc_" + peripheral.getType();
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
                for (li.cil.oc.api.network.ManagedEnvironment fileSystem : fileSystems.values()) {
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

            private String mount(final String path, final li.cil.oc.api.network.ManagedEnvironment fileSystem) {
                fileSystems.put(path, fileSystem);
                context.node().connect(fileSystem.node());
                return path;
            }

            @Override
            public void unmount(final String location) {
                final li.cil.oc.api.network.ManagedEnvironment fileSystem = fileSystems.remove(location);
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
            public @NotNull Map<String, dan200.computercraft.api.peripheral.IPeripheral> getAvailablePeripherals() {
                return Map.of(owner.node().address(), owner.peripheral);
            }

            @Override
            public dan200.computercraft.api.peripheral.IPeripheral getAvailablePeripheral(@NotNull String name) {
                return owner.node().address().equals(name) ? owner.peripheral : null;
            }

            @Override
            @SuppressWarnings("DataFlowIssue")
            public @NotNull WorkMonitor getMainThreadMonitor() {
                return null;
            }
        }

        public static final class UnsupportedLuaContext implements ILuaContext {
            private static final UnsupportedLuaContext Instance = new UnsupportedLuaContext();

            private UnsupportedLuaContext() {
            }

            public static UnsupportedLuaContext instance() {
                return Instance;
            }

            @Override
            public long issueMainThreadTask(@NotNull LuaTask task) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
