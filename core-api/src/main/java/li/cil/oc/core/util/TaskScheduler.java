package li.cil.oc.core.util;

@FunctionalInterface
public interface TaskScheduler {
    void schedule(Runnable task);
}
