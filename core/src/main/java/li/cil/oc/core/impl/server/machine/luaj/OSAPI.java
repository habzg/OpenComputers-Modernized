package li.cil.oc.core.impl.server.machine.luaj;

import li.cil.oc.core.util.GameTimeFormatter;
import li.cil.repack.org.luaj.vm2.LuaValue;

public class OSAPI extends LuaJAPI {
    public OSAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    private static int getField(LuaValue table, String key, int d) {
        LuaValue res = table.get(key);
        if (!res.isint()) {
            if (d < 0) throw new IllegalArgumentException("field '" + key + "' missing in date table");
            return d;
        }
        return res.toint();
    }

    @Override
    public void initialize() {
        LuaValue os = LuaValue.tableOf();

        os.set("clock", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(machine.cpuTime())));

        os.set("date", ScalaClosure.wrapClosure(args -> {
            String format = (args.narg() > 0 && args.isstring(1)) ? args.tojstring(1) : "%d/%m/%y %H:%M:%S";
            double time = (args.narg() > 1 && args.isnumber(2)) ? args.todouble(2) : (machine.worldTime() + 6000) * 60 * 60 / 1000.0;

            GameTimeFormatter.DateTime dt = GameTimeFormatter.parse(time);

            if (format.startsWith("!"))
                format = format.substring(1);

            if (format.equals("*t")) {
                LuaValue table = LuaValue.tableOf(0, 8);
                table.set("year", LuaValue.valueOf(dt.year()));
                table.set("month", LuaValue.valueOf(dt.month()));
                table.set("day", LuaValue.valueOf(dt.day()));
                table.set("hour", LuaValue.valueOf(dt.hour()));
                table.set("min", LuaValue.valueOf(dt.minute()));
                table.set("sec", LuaValue.valueOf(dt.second()));
                table.set("wday", LuaValue.valueOf(dt.weekDay()));
                table.set("yday", LuaValue.valueOf(dt.yearDay()));
                return table;
            } else {
                return LuaValue.valueOf(GameTimeFormatter.format(format, dt));
            }
        }));

        os.set("time", ScalaClosure.wrapClosure(args -> {
            if (args.isnoneornil(1)) {
                return LuaValue.valueOf((machine.worldTime() + 6000) * 60 * 60 / 1000.0);
            } else {
                LuaValue table = args.checktable(1);

                int sec = getField(table, "sec", 0);
                int min = getField(table, "min", 0);
                int hour = getField(table, "hour", 12);
                int day = getField(table, "day", -1);
                int month = getField(table, "month", -1);
                int year = getField(table, "year", -1);

                Integer time = GameTimeFormatter.mktime(year, month, day, hour, min, sec);
                return time != null ? LuaValue.valueOf(time) : LuaValue.NIL;
            }
        }));

        lua().set("os", os);
    }
}
