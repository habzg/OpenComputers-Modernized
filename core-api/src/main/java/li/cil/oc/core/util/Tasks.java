package li.cil.oc.core.util;

public final class Tasks {
    private static TaskScheduler scheduler = task -> {
    };

    public static void setScheduler(TaskScheduler s) {
        scheduler = s;
    }

    public static void schedule(Runnable task) {
        scheduler.schedule(task);
    }
}
