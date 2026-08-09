package li.cil.oc.core.impl.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.prefab.AbstractValue;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class ConverterLuaObject implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof IDynamicLuaObject) {
            output.put("value", new LuaObjectValue((IDynamicLuaObject) value));
        }
    }

    public static final class LuaObjectValue extends AbstractValue implements ManagedPeripheral {
        private CallableHelper helper;
        private IDynamicLuaObject value;

        @SuppressWarnings("unused")
        public LuaObjectValue() {
        }

        public LuaObjectValue(final IDynamicLuaObject value) {
            this.value = value;
            helper = new CallableHelper(value.getMethodNames());
        }

        @Override
        public String[] methods() {
            if (value != null) return value.getMethodNames();
            return new String[0];
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Arguments args) {
            if (value != null && helper != null) {
                final int index = helper.methodIndex(method);
                final Object[] argArray = helper.convertArguments(args);
                try {
                    var luaContext = new BridgedLuaContext();
                    var result = value.callMethod(
                            luaContext,
                            index,
                            new BridgedArguments(argArray));
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
                } catch (Exception e) {
                    return new Object[]{null, e.getMessage() != null ? e.getMessage() : e.toString()};
                }
            }
            return new Object[]{null, "ComputerCraft userdata cannot be persisted"};
        }
    }

    static final class BridgedArguments implements IArguments {
        private final Object[] args;
        private final int offset;

        BridgedArguments(final Object[] args) {
            this(args, 0);
        }

        private BridgedArguments(final Object[] args, final int offset) {
            this.args = args;
            this.offset = offset;
        }

        @Override
        public int count() {
            return Math.max(0, args.length - offset);
        }

        @Override
        public Object get(final int index) {
            final int realIndex = index + offset;
            if (realIndex < 0 || realIndex >= args.length) return null;
            return args[realIndex];
        }

        @Override
        public @NotNull String getType(final int index) {
            Object o = get(index);
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
        public @NotNull IArguments drop(final int count) {
            return new BridgedArguments(args, offset + count);
        }
    }

    static final class BridgedLuaContext implements ILuaContext {
        private long nextTaskId = 0;
        private Object[] pendingTaskResult = null;

        @Override
        public long issueMainThreadTask(@NotNull final LuaTask task) {
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
        public @NotNull MethodResult executeMainThreadTask(@NotNull final LuaTask task) throws LuaException {
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
