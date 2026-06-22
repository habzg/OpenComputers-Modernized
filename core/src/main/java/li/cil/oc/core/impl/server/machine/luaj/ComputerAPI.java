package li.cil.oc.core.impl.server.machine.luaj;

import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.Connector;
import li.cil.oc.core.impl.Settings;
import li.cil.repack.org.luaj.vm2.LuaValue;
import net.minecraft.world.item.ItemStack;

public class ComputerAPI extends LuaJAPI {
    public ComputerAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue computer = LuaValue.tableOf();

        computer.set("realTime", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(System.currentTimeMillis() / 1000.0)));

        computer.set("uptime", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(machine.upTime())));

        computer.set("address", ScalaClosure.wrapClosure(args -> {
            String address = node().address();
            return address != null ? LuaValue.valueOf(address) : LuaValue.NIL;
        }));

        computer.set("freeMemory", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(owner.memory / 2)));

        computer.set("totalMemory", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(owner.memory)));

        computer.set("pushSignal", ScalaClosure.wrapClosure(args -> {
            Object[] signalArgs = ScalaClosure.toSimpleJavaObjects(args, 2).toArray();
            return LuaValue.valueOf(machine.signal(args.checkjstring(1), signalArgs));
        }));

        computer.set("tmpAddress", ScalaClosure.wrapClosure(args -> {
            Object address = machine.tmpAddress();
            return address != null ? LuaValue.valueOf(address.toString()) : LuaValue.NIL;
        }));

        computer.set("users", ScalaClosure.wrapClosure(args -> {
            String[] users = machine.users();
            LuaValue[] values = new LuaValue[users.length];
            for (int i = 0; i < users.length; i++) {
                values[i] = LuaValue.valueOf(users[i]);
            }
            return LuaValue.varargsOf(values);
        }));

        computer.set("addUser", ScalaClosure.wrapClosure(args -> {
            machine.addUser(args.checkjstring(1));
            return LuaValue.TRUE;
        }));

        computer.set("removeUser", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(machine.removeUser(args.checkjstring(1)))));

        computer.set("energy", ScalaClosure.wrapClosure(args -> {
            if (Settings.get().ignorePower)
                return LuaValue.valueOf(Double.POSITIVE_INFINITY);
            else
                return LuaValue.valueOf(((Connector) node()).globalBuffer());
        }));

        computer.set("maxEnergy", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(((Connector) node()).globalBufferSize())));

        computer.set("getArchitectures", ScalaClosure.wrapClosure(args -> {
            Iterable<ItemStack> components = machine.host().internalComponents();
            for (ItemStack stack : components) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof MutableProcessor) {
                    java.util.Collection<Class<? extends Architecture>> archs = ((MutableProcessor) driver).allArchitectures();
                    LuaValue[] values = new LuaValue[archs.size()];
                    int idx = 0;
                    for (Class<? extends Architecture> arch : archs) {
                        values[idx++] = LuaValue.valueOf(li.cil.oc.api.Machine.getArchitectureName(arch));
                    }
                    return LuaValue.listOf(values);
                } else if (driver instanceof Processor) {
                    return LuaValue.listOf(new LuaValue[]{LuaValue.valueOf(li.cil.oc.api.Machine.getArchitectureName(((Processor) driver).architecture(stack)))});
                }
            }
            return LuaValue.tableOf();
        }));

        computer.set("getArchitecture", ScalaClosure.wrapClosure(args -> {
            Iterable<ItemStack> components = machine.host().internalComponents();
            for (ItemStack stack : components) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof Processor) {
                    return LuaValue.valueOf(li.cil.oc.api.Machine.getArchitectureName(((Processor) driver).architecture(stack)));
                }
            }
            return LuaValue.NONE;
        }));

        computer.set("setArchitecture", ScalaClosure.wrapClosure(args -> {
            String archName = args.checkjstring(1);
            Iterable<ItemStack> components = machine.host().internalComponents();
            for (ItemStack stack : components) {
                Object driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof MutableProcessor mp) {
                    Class<? extends Architecture> currentArch = mp.architecture(stack);
                    for (Class<? extends Architecture> arch : mp.allArchitectures()) {
                        if (li.cil.oc.api.Machine.getArchitectureName(arch).equals(archName)) {
                            if (!arch.equals(currentArch)) {
                                mp.setArchitecture(stack, arch);
                                return LuaValue.TRUE;
                            } else {
                                return LuaValue.FALSE;
                            }
                        }
                    }
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("unknown architecture"));
                }
            }
            return LuaValue.NONE;
        }));

        lua().set("computer", computer);
    }
}
