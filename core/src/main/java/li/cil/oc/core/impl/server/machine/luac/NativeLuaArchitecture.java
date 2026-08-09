package li.cil.oc.core.impl.server.machine.luac;

import com.google.common.base.Strings;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedLuaState;
import li.cil.oc.core.impl.util.SaveHandlerDelegate;
import li.cil.oc.core.util.MachineStateHelper;
import li.cil.repack.com.naef.jnlua.LuaGcMetamethodException;
import li.cil.repack.com.naef.jnlua.LuaMemoryAllocationException;
import li.cil.repack.com.naef.jnlua.LuaRuntimeException;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NativeLuaArchitecture implements Architecture {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLuaArchitecture.class);
    protected final li.cil.oc.api.machine.Machine machine;
    private final PersistenceAPI persistence;
    private final NativeLuaAPI[] apis;
    LuaState lua = null;
    int kernelMemory = 0;
    double ramScale = 1.0;

    public NativeLuaArchitecture(li.cil.oc.api.machine.Machine machine) {
        this.machine = machine;
        this.persistence = new PersistenceAPI(this);
        this.apis = new NativeLuaAPI[]{
                new ComponentAPI(this),
                new ComputerAPI(this),
                new OSAPI(this),
                new SystemAPI(this),
                new UnicodeAPI(this),
                new UserdataAPI(this),
                persistence,
        };
    }

    protected abstract LuaStateFactory factory();

    int invoke(java.util.function.Supplier<Object[]> f) {
        try {
            Object[] results = f.get();
            lua.pushBoolean(true);
            if (results != null) {
                for (Object result : results) {
                    ExtendedLuaState.pushValue(lua, result);
                }
                return 1 + results.length;
            } else {
                return 1;
            }
        } catch (Throwable e) {
            if (OCSettings.get().logLuaCallbackErrors && !(e instanceof LimitReachedException)) {
                LOGGER.warn("Exception in Lua callback.", e);
            }
            if (e instanceof LimitReachedException) {
                return 0;
            }
            Throwable cause = e;
            while (cause instanceof RuntimeException rte && rte.getCause() != null && rte.getCause() != rte) {
                cause = rte.getCause();
            }
            if (cause instanceof IllegalArgumentException iae && iae.getMessage() != null) {
                lua.pushBoolean(false);
                lua.pushString(iae.getMessage());
                return 2;
            }
            String msg = cause.getMessage();
            if (msg != null) {
                lua.pushBoolean(true);
                lua.pushNil();
                lua.pushString(msg);
                if (OCSettings.get().logLuaCallbackErrors) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    lua.pushString(sw.toString());
                    return 4;
                }
                return 3;
            }
            switch (cause) {
                case IndexOutOfBoundsException ignored -> {
                    lua.pushBoolean(false);
                    lua.pushString("index out of bounds");
                    return 2;
                }
                case IllegalArgumentException ignored -> {
                    lua.pushBoolean(false);
                    lua.pushString("bad argument");
                    return 2;
                }
                case NoSuchMethodException ignored -> {
                    lua.pushBoolean(false);
                    lua.pushString("no such method");
                    return 2;
                }
                case FileNotFoundException ignored -> {
                    lua.pushBoolean(true);
                    lua.pushNil();
                    lua.pushString("file not found");
                    return 3;
                }
                case SecurityException ignored -> {
                    lua.pushBoolean(true);
                    lua.pushNil();
                    lua.pushString("access denied");
                    return 3;
                }
                case IOException ignored -> {
                    lua.pushBoolean(true);
                    lua.pushNil();
                    lua.pushString("i/o error");
                    return 3;
                }
                case UnsupportedOperationException ignored -> {
                    lua.pushBoolean(false);
                    lua.pushString("unsupported operation");
                    return 2;
                }
                default -> {
                }
            }
            LOGGER.warn("Unexpected error in Lua callback.", e);
            lua.pushBoolean(true);
            lua.pushNil();
            lua.pushString("unknown error");
            return 3;
        }
    }

    int documentation(java.util.function.Supplier<String> f) {
        try {
            String doc = f.get();
            if (Strings.isNullOrEmpty(doc)) lua.pushNil();
            else lua.pushString(doc);
            return 1;
        } catch (Throwable t) {
            lua.pushNil();
            lua.pushString(t.getMessage() != null ? t.getMessage() : t.toString());
            return 2;
        }
    }

    @Override
    public boolean isInitialized() {
        return kernelMemory > 0;
    }

    @Override
    public boolean recomputeMemory(Iterable<ItemStack> components) {
        int memoryBytes = memoryInBytes(components);
        if (lua != null && OCSettings.get().limitMemory) {
            lua.setTotalMemory(Integer.MAX_VALUE);
            if (kernelMemory > 0) {
                lua.setTotalMemory(kernelMemory + (int) Math.ceil(memoryBytes * ramScale));
            }
        }
        return memoryBytes > 0;
    }

    private int memoryInBytes(Iterable<ItemStack> components) {
        double acc = 0.0;
        for (ItemStack stack : components) {
            Object driver = li.cil.oc.api.API.driver.driverFor(stack);
            if (driver instanceof Memory) {
                acc += ((Memory) driver).amount(stack) * 1024;
            }
        }
        return Math.clamp((int) acc, 0, OCSettings.get().maxTotalRam);
    }

    @Override
    public void runSynchronized() {
        assert lua.getTop() == 2;
        assert lua.isThread(1);
        assert lua.isFunction(2);

        try {
            lua.call(0, 1);
            lua.checkType(2, LuaType.TABLE);
        } catch (LuaMemoryAllocationException e) {
            throw new java.lang.OutOfMemoryError("not enough memory");
        }
    }

    @Override
    public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
        try {
            assert lua.isThread(1);

            int results;
            if (isSynchronizedReturn) {
                assert lua.getTop() == 2;
                assert lua.isTable(2);
                results = lua.resume(1, 1);
            } else if (kernelMemory == 0) {
                if (lua.resume(1, 0) > 0) {
                    results = 0;
                } else {
                    lua.gc(LuaState.GcAction.COLLECT, 0);
                    kernelMemory = Math.max(lua.getTotalMemory() - lua.getFreeMemory(), 1);
                    recomputeMemory(machine.host().internalComponents());
                    lua.pushInteger(0);
                    results = 1;
                }
            } else {
                li.cil.oc.api.machine.Signal signal = machine.popSignal();
                if (signal != null) {
                    lua.pushString(signal.name());
                    for (Object arg : signal.args()) {
                        ExtendedLuaState.pushValue(lua, arg);
                    }
                    results = lua.resume(1, 1 + signal.args().length);
                } else {
                    results = lua.resume(1, 0);
                }
            }

            if (lua.status(1) == LuaState.YIELD) {
                if (results == 1 && lua.isFunction(2)) {
                    return new ExecutionResult.SynchronizedCall();
                } else if (results == 1 && lua.isBoolean(2)) {
                    return new ExecutionResult.Shutdown(lua.toBoolean(2));
                } else {
                    int ticks = (results == 1 && lua.isNumber(2)) ? (int) (lua.toNumber(2) * 20) : Integer.MAX_VALUE;
                    lua.pop(results);
                    return new ExecutionResult.Sleep(ticks);
                }
            } else {
                assert lua.isThread(1);
                if (!lua.isBoolean(2) || !(lua.isString(3) || lua.isNoneOrNil(3))) {
                    LOGGER.warn("Kernel returned unexpected results.");
                }
                if (lua.toBoolean(2)) {
                    LOGGER.warn("Kernel stopped unexpectedly.");
                    return new ExecutionResult.Shutdown(false);
                } else {
                    if (OCSettings.get().limitMemory) {
                        lua.setTotalMemory(Integer.MAX_VALUE);
                    }
                    String error;
                    if (lua.isJavaObjectRaw(3)) {
                        error = lua.toJavaObjectRaw(3).toString();
                    } else {
                        error = lua.toString(3);
                    }
                    return new ExecutionResult.Error(Objects.requireNonNullElse(error, "unknown error"));
                }
            }
        } catch (LuaRuntimeException e) {
            LOGGER.warn("Kernel crashed. This is a bug!\n{}\tat {}", e, java.util.Arrays.stream(e.getLuaStackTrace()).map(Object::toString).collect(java.util.stream.Collectors.joining("\n\tat ")));
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        } catch (LuaGcMetamethodException e) {
            if (e.getMessage() != null) return new ExecutionResult.Error("kernel panic:\n" + e.getMessage());
            else return new ExecutionResult.Error("kernel panic:\nerror in garbage collection metamethod");
        } catch (LuaMemoryAllocationException e) {
            return new ExecutionResult.Error("not enough memory");
        } catch (java.lang.Error e) {
            if ("not enough memory".equals(e.getMessage())) return new ExecutionResult.Error("not enough memory");
            throw e;
        }
    }

    @Override
    public void onSignal() {
    }

    @Override
    public boolean initialize() {
        LuaState state = factory().createState();
        if (state == null) {
            lua = null;
            machine.crash("native libraries not available");
            return false;
        }
        lua = state;
        ramScale = lua.getPointerWidth() >= 8 ? OCSettings.get().ramScaleFor64Bit : 1.0;

        for (NativeLuaAPI api : apis) {
            api.initialize();
        }

        try {
            lua.load(NativeLuaArchitecture.class.getResourceAsStream(OCSettings.scriptPath + "machine.lua"), "=machine", "t");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lua.newThread();

        return true;
    }

    @Override
    public void onConnect() {
    }

    @Override
    public void close() {
        if (lua != null) {
            if (OCSettings.get().limitMemory) {
                lua.setTotalMemory(Integer.MAX_VALUE);
            }
            lua.close();
        }
        lua = null;
        kernelMemory = 0;
    }

    @Override
    public void load(CompoundTag nbt) {
        if (!machine.isRunning()) return;

        if (OCSettings.get().limitMemory) {
            lua.setTotalMemory(Integer.MAX_VALUE);
        }

        try {
            lua.setTop(0);

            persistence.unpersist(SaveHandlerDelegate.get().load(nbt, machine.node().address() + "_kernel"));
            if (!lua.isThread(1)) {
                throw new LuaRuntimeException("Invalid kernel.");
            }

            if (MachineStateHelper.get().isInSynchronizedCall(machine)) {
                persistence.unpersist(SaveHandlerDelegate.get().load(nbt, machine.node().address() + "_stack"));
                if (!lua.isFunction(2) && !lua.isTable(2)) {
                    throw new LuaRuntimeException("Invalid stack.");
                }
            }

            kernelMemory = (int) (nbt.getInt("kernelMemory") * ramScale);

            for (NativeLuaAPI api : apis) {
                api.load(nbt, machine.host().level().registryAccess());
            }

            try {
                lua.gc(LuaState.GcAction.COLLECT, 0);
            } catch (Throwable t) {
                LOGGER.warn("Error cleaning up loaded computer during load @ {}. This either means the server is badly overloaded or a user created an evil __gc method, accidentally or not.", machine.host().machinePosition());
                machine.crash("error in garbage collector, most likely __gc method timed out");
            }
        } catch (LuaRuntimeException e) {
            throw new RuntimeException(e + (e.getLuaStackTrace().length == 0 ? "" : "\tat " + java.util.Arrays.stream(e.getLuaStackTrace()).map(Object::toString).collect(java.util.stream.Collectors.joining("\n\tat "))), e);
        }

        recomputeMemory(machine.host().internalComponents());
    }

    @Override
    public void save(CompoundTag nbt) {
        if (OCSettings.get().limitMemory) {
            lua.setTotalMemory(Integer.MAX_VALUE);
        }

        try {
            assert lua.isThread(1);

            SaveHandlerDelegate.get().scheduleSave(machine.host(), nbt, machine.node().address() + "_kernel", persistence.persist(1));

            if (MachineStateHelper.get().isInSynchronizedCall(machine)) {
                assert lua.isFunction(2) || lua.isTable(2);
                SaveHandlerDelegate.get().scheduleSave(machine.host(), nbt, machine.node().address() + "_stack", persistence.persist(2));
            }

            nbt.putInt("kernelMemory", (int) Math.ceil(kernelMemory / ramScale));

            for (NativeLuaAPI api : apis) {
                api.save(nbt, machine.host().level().registryAccess());
            }

            try {
                lua.gc(LuaState.GcAction.COLLECT, 0);
            } catch (Throwable t) {
                LOGGER.warn("Error cleaning up loaded computer during save @ {}. This either means the server is badly overloaded or a user created an evil __gc method, accidentally or not.", machine.host().machinePosition());
                machine.crash("error in garbage collector, most likely __gc method timed out");
            }
        } catch (LuaRuntimeException e) {
            LOGGER.warn("Could not persist computer @ {}.\n{}{}", machine.host().machinePosition(), e, e.getLuaStackTrace().length == 0 ? "" : "\tat " + java.util.Arrays.stream(e.getLuaStackTrace()).map(Object::toString).collect(java.util.stream.Collectors.joining("\n\tat ")));
            nbt.remove("state");
        } catch (LuaGcMetamethodException e) {
            LOGGER.warn("Could not persist computer @ {}.\n{}", machine.host().machinePosition(), e);
            nbt.remove("state");
        }

        recomputeMemory(machine.host().internalComponents());
    }
}
