package li.cil.oc.core.impl.integration.computercraft;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import li.cil.oc.api.machine.Arguments;

@SuppressWarnings("unused")
public final class CallableHelper {
    private final List<String> _methods;

    public CallableHelper(final String[] methods) {
        _methods = Arrays.asList(methods);
    }

    public int methodIndex(final String method) {
        final int index = _methods.indexOf(method);
        if (index < 0) {
            throw new RuntimeException(new NoSuchMethodException());
        }
        return index;
    }

    public Object[] convertArguments(final Arguments args) {
        var list = new ArrayList<>();
        for (var arg : args) list.add(arg);
        var argArray = list.toArray();
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i] instanceof byte[]) {
                argArray[i] = new String((byte[]) argArray[i], StandardCharsets.UTF_8);
            }
        }
        return argArray;
    }
}
