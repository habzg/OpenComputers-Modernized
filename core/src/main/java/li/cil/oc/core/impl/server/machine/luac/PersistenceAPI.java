package li.cil.oc.core.impl.server.machine.luac;

import java.util.ArrayList;
import java.util.UUID;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class PersistenceAPI extends NativeLuaAPI {
    private String persistKey;

    public PersistenceAPI(NativeLuaArchitecture owner) {
        super(owner);
        this.persistKey = "__persist" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void initialize() {
        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushString(persistKey);
            return 1;
        });
        lua().setGlobal("persistKey");

        if (OCSettings.get().allowPersistence) {
            lua().newTable();
            lua().newTable();

            int perms = lua().getTop() - 1;
            int uperms = lua().getTop();

            lua().pushString("_G");
            lua().getGlobal("_G");

            flattenAndStore(perms, uperms);

            lua().setField(lua().getRegistryIndex(), "uperms");
            lua().setField(lua().getRegistryIndex(), "perms");

            ExtendedLuaState.pushScalaFunction(lua(), persistL -> {
                persistL.getGlobal("userdata");
                persistL.getField(-1, "save");
                persistL.pushValue(1);
                persistL.call(1, 2);
                persistL.remove(-4);
                persistL.remove(-3);
                persistL.getField(persistL.getRegistryIndex(), "oc_persist_factory");
                persistL.pushValue(-3);
                persistL.pushValue(-3);
                persistL.call(2, 1);
                persistL.pop(2);
                return 1;
            });
            lua().setField(lua().getRegistryIndex(), "oc_persist");

            lua().load(
                    "return function(cls, data) return function() return userdata.load(cls, data) end end",
                    "=persist_factory"
            );
            lua().setField(lua().getRegistryIndex(), "oc_persist_factory");
        }
    }

    private void flattenAndStore(int perms, int uperms) {
        if (lua().isFunction(-1) || lua().isTable(-1)) {
            lua().pushValue(-2);
            lua().getTable(uperms);
            assert lua().isNil(-1) : "duplicate permanent value named " + lua().toString(-3);
            lua().pop(1);

            lua().pushValue(-1);
            lua().getTable(perms);
            boolean isNew = lua().isNil(-1);
            lua().pop(1);

            if (isNew) {
                lua().pushValue(-1);
                lua().pushValue(-3);
                lua().rawSet(perms);

                lua().pushValue(-2);
                lua().pushValue(-2);
                lua().rawSet(uperms);

                if (lua().isTable(-1)) {
                    String key = lua().toString(-2);
                    ArrayList<String> childKeys = new ArrayList<>();
                    lua().pushNil();
                    while (lua().next(-2)) {
                        lua().pop(1);
                        childKeys.add(lua().toString(-1));
                    }
                    childKeys.sort(String::compareTo);
                    for (String childKey : childKeys) {
                        lua().pushString(key + "." + childKey);
                        lua().getField(-2, childKey);
                        flattenAndStore(perms, uperms);
                    }
                }
            }
        }
        lua().pop(2);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains("persistKey")) {
            persistKey = nbt.getString("persistKey");
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putString("persistKey", persistKey);
    }

    public void configure() {
        lua().getGlobal("eris");

        lua().getField(-1, "settings");
        lua().pushString("spkey");
        lua().pushString(persistKey);
        lua().call(2, 0);

        lua().getField(-1, "settings");
        lua().pushString("path");
        lua().pushBoolean(OCSettings.get().debugPersistence);
        lua().call(2, 0);

        lua().pop(1);
        lua().pushJavaObjectRaw(new li.cil.oc.api.prefab.AbstractValue() {
        });
        if (lua().getMetatable(-1)) {
            lua().getField(lua().getRegistryIndex(), "oc_persist");
            lua().setField(-2, persistKey);
            lua().pop(1);
        }
        lua().pop(1);
    }

    public byte[] persist(int index) {
        if (OCSettings.get().allowPersistence) {
            configure();
            try {
                lua().gc(LuaState.GcAction.STOP, 0);
                lua().getGlobal("eris");
                lua().getField(-1, "persist");
                if (lua().isFunction(-1)) {
                    lua().getField(lua().getRegistryIndex(), "perms");
                    lua().pushValue(index);
                    try {
                        lua().call(2, 1);
                    } catch (Throwable e) {
                        lua().pop(1);
                        throw e;
                    }
                    if (lua().isString(-1)) {
                        byte[] result = lua().toByteArray(-1);
                        lua().pop(2);
                        return result;
                    }
                }
                lua().pop(2);
            } finally {
                lua().gc(LuaState.GcAction.RESTART, 0);
            }
        }
        return new byte[0];
    }

    public void unpersist(byte[] value) {
        if (OCSettings.get().allowPersistence) {
            configure();
            try {
                lua().gc(LuaState.GcAction.STOP, 0);
                lua().getGlobal("eris");
                lua().getField(-1, "unpersist");
                if (lua().isFunction(-1)) {
                    lua().getField(lua().getRegistryIndex(), "uperms");
                    lua().pushByteArray(value);
                    lua().call(2, 1);
                    lua().insert(-2);
                    lua().pop(1);
                    return;
                }
                lua().pop(1);
            } finally {
                lua().gc(LuaState.GcAction.RESTART, 0);
            }
        }
    }
}
