package li.cil.oc.core.impl.server.machine.luac;

import li.cil.oc.core.impl.util.ExtendedLuaState;
import li.cil.oc.core.impl.util.FontUtils;
import li.cil.oc.core.util.ExtendedUnicodeHelper;

public class UnicodeAPI extends NativeLuaAPI {
    public UnicodeAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i <= l.getTop(); i++) {
                builder.appendCodePoint(l.checkInt32(i));
            }
            l.pushString(builder.toString());
            return 1;
        });
        lua().setField(-2, "char");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String s = l.checkString(1);
            l.pushInteger(ExtendedUnicodeHelper.length(s));
            return 1;
        });
        lua().setField(-2, "len");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushString(l.checkString(1).toLowerCase());
            return 1;
        });
        lua().setField(-2, "lower");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushString(ExtendedUnicodeHelper.reverse(l.checkString(1)));
            return 1;
        });
        lua().setField(-2, "reverse");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String string = l.checkString(1);
            int sLength = ExtendedUnicodeHelper.length(string);
            int start;
            int rawStart = l.checkInt32(2);
            if (rawStart < 0) {
                start = string.offsetByCodePoints(string.length(), Math.max(rawStart, -sLength));
            } else if (rawStart == 0) {
                start = 0;
            } else {
                start = string.offsetByCodePoints(0, Math.min(rawStart - 1, sLength));
            }
            int end;
            if (l.getTop() > 2) {
                int rawEnd = l.checkInt32(3);
                if (rawEnd < 0) {
                    end = string.offsetByCodePoints(string.length(), Math.max(rawEnd + 1, -sLength));
                } else {
                    end = string.offsetByCodePoints(0, Math.min(rawEnd, sLength));
                }
            } else {
                end = string.length();
            }
            if (end <= start) l.pushString("");
            else l.pushString(string.substring(start, end));
            return 1;
        });
        lua().setField(-2, "sub");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushString(l.checkString(1).toUpperCase());
            return 1;
        });
        lua().setField(-2, "upper");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushBoolean(FontUtils.wcwidth(l.checkString(1).codePointAt(0)) > 1);
            return 1;
        });
        lua().setField(-2, "isWide");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            l.pushInteger(FontUtils.wcwidth(l.checkString(1).codePointAt(0)));
            return 1;
        });
        lua().setField(-2, "charWidth");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String value = l.checkString(1);
            int sum = value.codePoints().map(ch -> Math.max(1, FontUtils.wcwidth(ch))).sum();
            l.pushInteger(sum);
            return 1;
        });
        lua().setField(-2, "wlen");

        ExtendedLuaState.pushScalaFunction(lua(), l -> {
            String value = l.checkString(1);
            int count = Math.toIntExact(l.checkInteger(2));
            int width = 0;
            int end = 0;
            while (width < count) {
                width += Math.max(1, FontUtils.wcwidth(value.codePointAt(end)));
                end = value.offsetByCodePoints(end, 1);
            }
            if (end > 1) l.pushString(value.substring(0, end - 1));
            else l.pushString("");
            return 1;
        });
        lua().setField(-2, "wtrunc");

        lua().setGlobal("unicode");
    }
}
