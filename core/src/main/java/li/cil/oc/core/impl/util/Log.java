package li.cil.oc.core.impl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {
    private static final Logger LOG = LoggerFactory.getLogger("OpenComputers");

    private Log() {
    }

    public static Logger get() {
        return LOG;
    }
}
