package li.cil.oc.core.impl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SafeThreadPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(SafeThreadPool.class);
    private final String name;
    private final int threads;
    private ScheduledExecutorService _threadPool;

    public SafeThreadPool(String name, int threads) {
        this.name = name;
        this.threads = threads;
    }

    public Future<?> withPool(Function<ScheduledExecutorService, Future<?>> f, boolean requiresPool) {
        if (_threadPool == null) {
            LOGGER.warn("Error handling file saving: Did the server never start?");
            if (requiresPool) {
                LOGGER.warn("Creating new thread pool.");
                newThreadPool();
            } else {
                return null;
            }
        } else if (_threadPool.isShutdown() || _threadPool.isTerminated()) {
            LOGGER.warn("Error handling file saving: Thread pool shut down!");
            if (requiresPool) {
                LOGGER.warn("Creating new thread pool.");
                newThreadPool();
            } else {
                return null;
            }
        }
        return f.apply(_threadPool);
    }

    public Future<?> withPool(Function<ScheduledExecutorService, Future<?>> f) {
        return withPool(f, true);
    }

    public void newThreadPool() {
        if (_threadPool != null && !_threadPool.isTerminated()) {
            _threadPool.shutdownNow();
        }
        _threadPool = ThreadPoolFactory.create(name, threads);
    }

    public void waitForCompletion() {
        withPool(threadPool -> {
            try {
                threadPool.shutdown();
                boolean terminated = threadPool.awaitTermination(15, TimeUnit.SECONDS);
                if (!terminated) {
                    LOGGER.warn("Warning: Completing all tasks has already taken 15 seconds!");
                    terminated = threadPool.awaitTermination(105, TimeUnit.SECONDS);
                    if (!terminated) {
                        LOGGER.error("Warning: Completing all tasks has already taken two minutes! Aborting");
                        threadPool.shutdownNow();
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.error("Error shutting down thread pool", e);
            }
            return null;
        }, false);
    }
}
