package li.cil.oc.core.impl.server.machine.luac;

import li.cil.oc.core.impl.util.ExtendedLuaState;
import li.cil.oc.core.util.GameTimeFormatter;
import li.cil.repack.com.naef.jnlua.LuaType;

public class OSAPI extends NativeLuaAPI {
    public OSAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    private static int getField(li.cil.repack.com.naef.jnlua.LuaState l, String key, int d) {
        l.getField(-1, key);
        Long res = l.toIntegerX(-1);
        l.pop(1);
        if (res == null) {
            if (d < 0) throw new IllegalArgumentException("field '" + key + "' missing in date table");
            return d;
        }
        return res.intValue();
    }

    @Override
    public void initialize() {
        lua().getGlobal("os");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushNumber(machine.cpuTime());
            return 1;
        });
        lua().setField(-2, "clock");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String format = (l.getTop() > 0 && l.isString(1)) ? l.toString(1) : "%d/%m/%y %H:%M:%S";
            double time = (l.getTop() > 1 && l.isNumber(2)) ? l.toNumber(2) : ((machine.worldTime() + 6000) * 60 * 60) / 1000.0;

            GameTimeFormatter.DateTime dt = GameTimeFormatter.parse(time);

            if (format.startsWith("!"))
                format = format.substring(1);

            if (format.equals("*t")) {
                l.newTable(0, 8);
                l.pushInteger(dt.year());
                l.setField(-2, "year");
                l.pushInteger(dt.month());
                l.setField(-2, "month");
                l.pushInteger(dt.day());
                l.setField(-2, "day");
                l.pushInteger(dt.hour());
                l.setField(-2, "hour");
                l.pushInteger(dt.minute());
                l.setField(-2, "min");
                l.pushInteger(dt.second());
                l.setField(-2, "sec");
                l.pushInteger(dt.weekDay());
                l.setField(-2, "wday");
                l.pushInteger(dt.yearDay());
                l.setField(-2, "yday");
            } else {
                l.pushString(GameTimeFormatter.format(format, dt));
            }
            return 1;
        });
        lua().setField(-2, "date");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            if (l.isNoneOrNil(1)) {
                l.pushNumber(((machine.worldTime() + 6000) * 60 * 60) / 1000.0);
            } else {
                l.checkType(1, LuaType.TABLE);
                l.setTop(1);

                int sec = getField(l, "sec", 0);
                int min = getField(l, "min", 0);
                int hour = getField(l, "hour", 12);
                int day = getField(l, "day", -1);
                int month = getField(l, "month", -1);
                int year = getField(l, "year", -1);

                Integer time = GameTimeFormatter.mktime(year, month, day, hour, min, sec);
                if (time != null) l.pushNumber(time);
                else l.pushNil();
            }
            return 1;
        });
        lua().setField(-2, "time");

        lua().pop(1);
    }
}
