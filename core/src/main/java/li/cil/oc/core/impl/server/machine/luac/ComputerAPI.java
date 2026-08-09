package li.cil.oc.core.impl.server.machine.luac;

import java.util.Collection;
import java.util.List;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.Connector;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedLuaState;
import net.minecraft.world.item.ItemStack;

public class ComputerAPI extends NativeLuaAPI {
    public ComputerAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    public void initialize() {
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushNumber(System.currentTimeMillis() / 1000.0);
            return 1;
        });
        lua().setField(-2, "realTime");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushNumber(machine.upTime());
            return 1;
        });
        lua().setField(-2, "uptime");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String address = node().address();
            if (address != null) l.pushString(address);
            else l.pushNil();
            return 1;
        });
        lua().setField(-2, "address");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushInteger((int) ((Math.min(l.getFreeMemory(), l.getTotalMemory() - owner.kernelMemory)) / owner.ramScale));
            return 1;
        });
        lua().setField(-2, "freeMemory");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushInteger((int) ((l.getTotalMemory() - owner.kernelMemory) / owner.ramScale));
            return 1;
        });
        lua().setField(-2, "totalMemory");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            List<Object> args = ExtendedLuaState.toSimpleJavaObjects(l, 2);
            l.pushBoolean(machine.signal(l.checkString(1), args.toArray()));
            return 1;
        });
        lua().setField(-2, "pushSignal");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String address = machine.tmpAddress();
            if (address == null) l.pushNil();
            else l.pushString(address);
            return 1;
        });
        lua().setField(-2, "tmpAddress");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String[] users = machine.users();
            for (String user : users) l.pushString(user);
            return users.length;
        });
        lua().setField(-2, "users");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String user = l.checkString(1);
            try {
                machine.addUser(user);
                l.pushBoolean(true);
                return 1;
            } catch (Throwable e) {
                l.pushNil();
                l.pushString(e.getMessage() != null ? e.getMessage() : e.toString());
                return 2;
            }
        });
        lua().setField(-2, "addUser");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushBoolean(machine.removeUser(l.checkString(1)));
            return 1;
        });
        lua().setField(-2, "removeUser");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            if (OCSettings.get().ignorePower)
                l.pushNumber(Double.POSITIVE_INFINITY);
            else
                l.pushNumber(((Connector) node()).globalBuffer());
            return 1;
        });
        lua().setField(-2, "energy");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushNumber(((Connector) node()).globalBufferSize());
            return 1;
        });
        lua().setField(-2, "maxEnergy");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Iterable<ItemStack> components = machine.host().internalComponents();
            for (ItemStack stack : components) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof MutableProcessor) {
                    Collection<Class<? extends Architecture>> archs = ((MutableProcessor) driver).allArchitectures();
                    ExtendedLuaState.pushValue(l, archs.stream().map(Machine::getArchitectureName).toArray());
                    return 1;
                } else if (driver instanceof Processor) {
                    ExtendedLuaState.pushValue(l, new Object[]{li.cil.oc.api.Machine.getArchitectureName(((Processor) driver).architecture(stack))});
                    return 1;
                }
            }
            l.newTable();
            return 1;
        });
        lua().setField(-2, "getArchitectures");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            Iterable<ItemStack> components = machine.host().internalComponents();
            for (ItemStack stack : components) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof Processor) {
                    l.pushString(li.cil.oc.api.Machine.getArchitectureName(((Processor) driver).architecture(stack)));
                    return 1;
                }
            }
            return 0;
        });
        lua().setField(-2, "getArchitecture");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String archName = l.checkString(1);
            Iterable<ItemStack> components = machine.host().internalComponents();
            for (ItemStack stack : components) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof MutableProcessor mp) {
                    Class<? extends Architecture> currentArch = mp.architecture(stack);
                    for (Class<? extends Architecture> arch : mp.allArchitectures()) {
                        if (li.cil.oc.api.Machine.getArchitectureName(arch).equals(archName)) {
                            if (!arch.equals(currentArch)) {
                                mp.setArchitecture(stack, arch);
                                l.pushBoolean(true);
                            } else {
                                l.pushBoolean(false);
                            }
                            return 1;
                        }
                    }
                    l.pushNil();
                    l.pushString("unknown architecture");
                    return 2;
                }
            }
            return 0;
        });
        lua().setField(-2, "setArchitecture");

        lua().setGlobal("computer");
    }
}
