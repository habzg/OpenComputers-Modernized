package li.cil.oc.core.impl.server.machine.luaj;

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Objects;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.core.impl.OCSettings;
import li.cil.repack.org.luaj.vm2.Globals;
import li.cil.repack.org.luaj.vm2.LuaError;
import li.cil.repack.org.luaj.vm2.LuaFunction;
import li.cil.repack.org.luaj.vm2.LuaThread;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.jse.JsePlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Architecture.Name("LuaJ")
public class LuaJLuaArchitecture implements Architecture {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaJLuaArchitecture.class);
    final li.cil.oc.api.machine.Machine machine;
    private final LuaJAPI[] apis;
    Globals lua = null;
    int memory = 0;
    private LuaThread thread = null;
    private LuaFunction synchronizedCall = null;
    private LuaValue synchronizedResult = null;
    private boolean doneWithInitRun = false;
    private int memoryCheckCounter = 0;

    public LuaJLuaArchitecture(li.cil.oc.api.machine.Machine machine) {
        this.machine = machine;
        this.apis = new LuaJAPI[]{
                new ComponentAPI(this),
                new ComputerAPI(this),
                new OSAPI(this),
                new SystemAPI(this),
                new UnicodeAPI(this),
                new UserdataAPI(this)
        };
    }

    Varargs invoke(java.util.function.Supplier<Object[]> f) {
        try {
            Object[] results = f.get();
            if (results != null) {
                LuaValue[] values = new LuaValue[results.length + 1];
                values[0] = LuaValue.TRUE;
                for (int i = 0; i < results.length; i++) {
                    values[i + 1] = ScalaClosure.toLuaValue(results[i]);
                }
                return LuaValue.varargsOf(values);
            } else {
                return LuaValue.TRUE;
            }
        } catch (Throwable e) {
            if (OCSettings.get().logLuaCallbackErrors && !(e instanceof LimitReachedException)) {
                LOGGER.warn("Exception in Lua callback.", e);
            }
            if (e instanceof LimitReachedException) {
                return LuaValue.NONE;
            }
            Throwable cause = e;
            while (cause instanceof RuntimeException rte && rte.getCause() != null && rte.getCause() != rte) {
                cause = rte.getCause();
            }
            if (cause instanceof IllegalArgumentException iae && iae.getMessage() != null) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(iae.getMessage()));
            }
            String msg = cause.getMessage();
            if (msg != null) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf(msg));
            }
            switch (cause) {
                case IndexOutOfBoundsException ignored -> {
                    return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("index out of bounds"));
                }
                case IllegalArgumentException ignored -> {
                    return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("bad argument"));
                }
                case NoSuchMethodException ignored -> {
                    return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("no such method"));
                }
                case java.io.FileNotFoundException ignored -> {
                    return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("file not found"));
                }
                case SecurityException ignored -> {
                    return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("access denied"));
                }
                case IOException ignored -> {
                    return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("i/o error"));
                }
                case UnsupportedOperationException ignored -> {
                    return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("unsupported operation"));
                }
                default -> {
                }
            }
            LOGGER.warn("Unexpected error in Lua callback.", e);
            return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("unknown error"));
        }
    }

    Varargs documentation(java.util.function.Supplier<String> f) {
        try {
            String doc = f.get();
            if (Strings.isNullOrEmpty(doc)) return LuaValue.NIL;
            else return LuaValue.valueOf(doc);
        } catch (Throwable t) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(t.getMessage() != null ? t.getMessage() : t.toString()));
        }
    }

    @Override
    public boolean isInitialized() {
        return doneWithInitRun;
    }

    @Override
    public boolean recomputeMemory(Iterable<ItemStack> components) {
        memory = memoryInBytes(components);
        return memory > 0;
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
        synchronizedResult = synchronizedCall.call();
        synchronizedCall = null;
    }

    @Override
    public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
        try {
            if (memory > 0 && ++memoryCheckCounter % 100 == 0) {
                long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                if (usedMem > memory) {
                    throw new RuntimeException("Computer exceeded memory limit");
                }
            }
            Varargs results;
            if (isSynchronizedReturn) {
                results = thread.resume(synchronizedResult);
                synchronizedResult = null;
            } else {
                if (!doneWithInitRun) {
                    results = thread.resume(LuaValue.NONE);
                    doneWithInitRun = true;
                    if (results.narg() == 1) {
                        results = LuaValue.varargsOf(LuaValue.TRUE, LuaValue.valueOf(0));
                    }
                } else {
                    li.cil.oc.api.machine.Signal signal = machine.popSignal();
                    if (signal != null) {
                        LuaValue[] signalArgs = new LuaValue[signal.args().length];
                        for (int i = 0; i < signal.args().length; i++) {
                            signalArgs[i] = ScalaClosure.toLuaValue(signal.args()[i]);
                        }
                        LuaValue[] combined = new LuaValue[signalArgs.length + 1];
                        combined[0] = LuaValue.valueOf(signal.name());
                        System.arraycopy(signalArgs, 0, combined, 1, signalArgs.length);
                        results = thread.resume(LuaValue.varargsOf(combined));
                    } else {
                        results = thread.resume(LuaValue.NONE);
                    }
                }
            }

            if (thread.state.status == LuaThread.STATUS_SUSPENDED) {
                if (results.narg() == 2 && results.isfunction(2)) {
                    synchronizedCall = results.checkfunction(2);
                    return new ExecutionResult.SynchronizedCall();
                } else if (results.narg() == 2 && results.type(2) == LuaValue.TBOOLEAN) {
                    return new ExecutionResult.Shutdown(results.toboolean(2));
                } else {
                    int ticks = (results.narg() == 2 && results.isnumber(2)) ? (int) (results.todouble(2) * 20) : Integer.MAX_VALUE;
                    return new ExecutionResult.Sleep(ticks);
                }
            } else {
                boolean isInnerError = results.type(2) == LuaValue.TBOOLEAN && (results.isstring(3) || results.isnoneornil(3));
                boolean isOuterError = results.isstring(2) || results.isnoneornil(2);
                if ((isOuterError && results.toboolean(1)) || (isInnerError && results.toboolean(2))) {
                    LOGGER.warn("Kernel stopped unexpectedly.");
                    return new ExecutionResult.Shutdown(false);
                } else {
                    String error;
                    if (isInnerError) {
                        if (results.isuserdata(3)) error = results.touserdata(3).toString();
                        else error = results.tojstring(3);
                    } else {
                        if (results.isuserdata(2)) error = results.touserdata(2).toString();
                        else error = results.tojstring(2);
                    }
                    return new ExecutionResult.Error(Objects.requireNonNullElse(error, "unknown error"));
                }
            }
        } catch (LuaError e) {
            LOGGER.warn("Kernel crashed. This is a bug!", e);
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        } catch (Throwable e) {
            LOGGER.warn("Unexpected error in kernel. This is a bug!", e);
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        }
    }

    @Override
    public void onSignal() {
    }

    @Override
    public boolean initialize() {
        lua = JsePlatform.debugGlobals();
        lua.set("package", LuaValue.NIL);
        lua.set("require", LuaValue.NIL);
        lua.set("io", LuaValue.NIL);
        lua.set("os", LuaValue.NIL);
        lua.set("luajava", LuaValue.NIL);

        lua.set("dofile", LuaValue.NIL);
        lua.set("loadfile", LuaValue.NIL);

        for (LuaJAPI api : apis) {
            api.initialize();
        }

        recomputeMemory(machine.host().internalComponents());

        if (OCSettings.get().limitMemory) {
            LOGGER.warn("LuaJ does not support per-state memory limits; memory allocation is bounded only by JVM heap.");
        }

        LuaValue kernel = lua.load(LuaJLuaArchitecture.class.getResourceAsStream(OCSettings.scriptPath + "machine.lua"), "=machine", "t", lua);
        thread = new LuaThread(lua, kernel);

        return true;
    }

    @Override
    public void onConnect() {
    }

    @Override
    public void close() {
        lua = null;
        thread = null;
        synchronizedCall = null;
        synchronizedResult = null;
        doneWithInitRun = false;
    }

    @Override
    public void load(CompoundTag nbt) {
        if (machine.isRunning()) {
            machine.stop();
            machine.start();
        }
    }

    @Override
    public void save(CompoundTag nbt) {
    }
}
