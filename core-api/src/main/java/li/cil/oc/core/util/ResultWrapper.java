package li.cil.oc.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResultWrapper {
    public static Object[] result(Object... args) {
        List<Object> unwrapped = new ArrayList<>();
        Collections.addAll(unwrapped, args);
        return unwrapped.toArray();
    }
}
