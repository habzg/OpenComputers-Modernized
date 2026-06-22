package li.cil.oc.core.impl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ClientTickScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTickScheduler.class);
    private static final List<Runnable> pending = new ArrayList<>();

    public static void schedule(Runnable r) {
        synchronized (pending) {
            pending.add(r);
        }
    }

    public static void runPending() {
        Runnable[] tasks;
        synchronized (pending) {
            tasks = pending.toArray(new Runnable[0]);
            pending.clear();
        }
        for (Runnable r : tasks) {
            try {
                r.run();
            } catch (Throwable t) {
                LOGGER.error("Error executing scheduled client tick task", t);
            }
        }
    }
}
