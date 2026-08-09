package li.cil.oc.core.impl.server.machine.luac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Value;
import li.cil.oc.core.impl.server.machine.ArgumentsImpl;
import li.cil.oc.core.impl.util.ExtendedLuaState;
import li.cil.oc.core.util.ConverterRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserdataAPI extends NativeLuaAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserdataAPI.class);

    private static final int MAX_USERDATA_SIZE = 1024 * 1024;

    public UserdataAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    public void initialize() {
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            CompoundTag nbt = new CompoundTag();
            Persistable persistable = (Persistable) l.toJavaObjectRaw(1);
            String className = persistable.getClass().getName();
            l.pushString(className);
            persistable.save(nbt, owner.machine.host().level().registryAccess());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            try {
                NbtIo.write(nbt, dos);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            l.pushByteArray(baos.toByteArray());
            return 2;
        });
        lua().setField(-2, "save");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            try {
                String className = l.toString(1);
                Class<?> clazz = Class.forName(className);
                Persistable persistable = (Persistable) clazz.getDeclaredConstructor().newInstance();
                byte[] data = l.toByteArray(2);
                if (data.length > MAX_USERDATA_SIZE) {
                    throw new IOException("userdata data exceeds maximum size");
                }
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                CompoundTag nbt = NbtIo.read(dis, net.minecraft.nbt.NbtAccounter.create(0x200000L));
                persistable.load(nbt, owner.machine.host().level().registryAccess());
                l.pushJavaObjectRaw(persistable);
                return 1;
            } catch (Throwable t) {
                LOGGER.warn("Error in userdata load function.", t);
                try {
                    throw t;
                } catch (ClassNotFoundException | IllegalAccessException | IOException | InstantiationException |
                         NoSuchMethodException | java.lang.reflect.InvocationTargetException e) {
                    if (e instanceof IOException ioe) throw new UncheckedIOException(ioe);
                    throw new RuntimeException(e);
                }
            }
        });
        lua().setField(-2, "load");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            Object[] args = ExtendedLuaState.toSimpleJavaObjects(l, 2).toArray();
            return owner.invoke(() -> ConverterRegistry.get().convert(new Object[]{value.apply(machine, new ArgumentsImpl(args))}));
        });
        lua().setField(-2, "apply");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            Object[] args = ExtendedLuaState.toSimpleJavaObjects(l, 2).toArray();
            return owner.invoke(() -> {
                value.unapply(machine, new ArgumentsImpl(args));
                return null;
            });
        });
        lua().setField(-2, "unapply");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            Object[] args = ExtendedLuaState.toSimpleJavaObjects(l, 2).toArray();
            return owner.invoke(() -> ConverterRegistry.get().convert(value.call(machine, new ArgumentsImpl(args))));
        });
        lua().setField(-2, "call");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            try {
                value.dispose(machine);
            } catch (Throwable t) {
                LOGGER.warn("Error in dispose method of userdata of type {}", value.getClass().getName(), t);
            }
            return 0;
        });
        lua().setField(-2, "dispose");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            Map<String, Callback> methods = machine.methods(value);
            l.newTable();
            for (Map.Entry<String, Callback> entry : methods.entrySet()) {
                l.pushString(entry.getKey());
                l.pushBoolean(entry.getValue().direct());
                l.rawSet(-3);
            }
            return 1;
        });
        lua().setField(-2, "methods");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            String method = l.checkString(2);
            Object[] args = ExtendedLuaState.toSimpleJavaObjects(l, 3).toArray();
            return owner.invoke(() -> machine.invoke(value, method, args));
        });
        lua().setField(-2, "invoke");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Value value = (Value) l.toJavaObjectRaw(1);
            String method = l.checkString(2);
            return owner.documentation(() -> {
                Map<String, Callback> methods = machine.methods(value);
                Callback cb = methods.get(method);
                return cb != null ? cb.doc() : null;
            });
        });
        lua().setField(-2, "doc");

        lua().setGlobal("userdata");
    }
}
