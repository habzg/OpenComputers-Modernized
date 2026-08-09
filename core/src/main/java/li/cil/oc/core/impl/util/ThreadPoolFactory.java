package li.cil.oc.core.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc.core.impl.OCSettings;
import org.jetbrains.annotations.NotNull;

public final class ThreadPoolFactory {

    public static final int priority;
    public static final List<SafeThreadPool> safePools = new ArrayList<>();

    static {
        int custom = OCSettings.get() != null ? OCSettings.get().threadPriority : -1;
        if (custom < 1) {
            priority = Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2;
        } else {
            priority = Math.clamp(custom, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        }
    }

    public static ScheduledExecutorService create(String name, int threads) {
        return Executors.newScheduledThreadPool(threads, new ThreadFactory() {
            private final String baseName = "OpenComputers-" + name + "-";
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final ThreadGroup group = Thread.currentThread().getThreadGroup();

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(group, r, baseName + threadNumber.getAndIncrement());
                if (!thread.isDaemon()) {
                    thread.setDaemon(true);
                }
                if (thread.getPriority() != priority) {
                    thread.setPriority(priority);
                }
                return thread;
            }
        });
    }

    public static SafeThreadPool createSafePool(String name, int threads) {
        SafeThreadPool handler = new SafeThreadPool(name, threads);
        safePools.add(handler);
        return handler;
    }
}
