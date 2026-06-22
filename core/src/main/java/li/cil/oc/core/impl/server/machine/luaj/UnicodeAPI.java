package li.cil.oc.core.impl.server.machine.luaj;

import li.cil.oc.core.impl.util.FontUtils;
import li.cil.oc.core.util.ExtendedUnicodeHelper;
import li.cil.repack.org.luaj.vm2.LuaValue;

public class UnicodeAPI extends LuaJAPI {
    public UnicodeAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue unicode = LuaValue.tableOf();

        unicode.set("lower", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(args.checkjstring(1).toLowerCase())));

        unicode.set("upper", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(args.checkjstring(1).toUpperCase())));

        unicode.set("char", ScalaClosure.wrapClosure(args -> {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i <= args.narg(); i++) {
                builder.appendCodePoint(args.checkint(i));
            }
            return LuaValue.valueOf(builder.toString());
        }));

        unicode.set("len", ScalaClosure.wrapClosure(args -> {
            String s = args.checkjstring(1);
            return LuaValue.valueOf(s.codePointCount(0, s.length()));
        }));

        unicode.set("reverse", ScalaClosure.wrapClosure(args -> LuaValue.valueOf(ExtendedUnicodeHelper.reverse(args.checkjstring(1)))));

        unicode.set("sub", ScalaClosure.wrapClosure(args -> {
            String string = args.checkjstring(1);
            int sLength = ExtendedUnicodeHelper.length(string);
            int rawStart = args.checkint(2);
            int start;
            if (rawStart < 0) {
                start = string.offsetByCodePoints(string.length(), Math.max(rawStart, -sLength));
            } else if (rawStart == 0) {
                start = 0;
            } else {
                start = string.offsetByCodePoints(0, Math.min(rawStart - 1, sLength));
            }
            int end;
            if (args.narg() > 2) {
                int rawEnd = args.checkint(3);
                if (rawEnd < 0) {
                    end = string.offsetByCodePoints(string.length(), Math.max(rawEnd + 1, -sLength));
                } else {
                    end = string.offsetByCodePoints(0, Math.min(rawEnd, sLength));
                }
            } else {
                end = string.length();
            }
            if (end <= start) return LuaValue.valueOf("");
            else return LuaValue.valueOf(string.substring(start, end));
        }));

        unicode.set("isWide", ScalaClosure.wrapClosure(args ->
                LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)) > 1)));

        unicode.set("charWidth", ScalaClosure.wrapClosure(args ->
                LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)))));

        unicode.set("wlen", ScalaClosure.wrapClosure(args -> {
            String value = args.checkjstring(1);
            int sum = value.codePoints().map(ch -> Math.max(1, FontUtils.wcwidth(ch))).sum();
            return LuaValue.valueOf(sum);
        }));

        unicode.set("wtrunc", ScalaClosure.wrapClosure(args -> {
            String value = args.checkjstring(1);
            int count = args.checkint(2);
            int width = 0;
            int end = 0;
            while (width < count) {
                width += Math.max(1, FontUtils.wcwidth(value.codePointAt(end)));
                end = value.offsetByCodePoints(end, 1);
            }
            if (end > 1) return LuaValue.valueOf(value.substring(0, end - 1));
            else return LuaValue.valueOf("");
        }));

        lua().set("unicode", unicode);
    }
}
