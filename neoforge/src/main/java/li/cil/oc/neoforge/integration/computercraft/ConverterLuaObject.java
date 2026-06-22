package li.cil.oc.neoforge.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.LuaException;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.prefab.AbstractValue;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
                    var result = value.callMethod(
                            DriverPeripheral.Environment.UnsupportedLuaContext.instance(),
                            index,
                            new IArguments() {
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
                    return new Object[]{null, "ComputerCraft yield is not supported"};
                } catch (LuaException e) {
                    return new Object[]{null, e.getMessage()};
                }
            }
            return new Object[]{null, "ComputerCraft userdata cannot be persisted"};
        }
    }
}
