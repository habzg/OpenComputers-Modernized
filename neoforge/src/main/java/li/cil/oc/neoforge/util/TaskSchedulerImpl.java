package li.cil.oc.neoforge.util;

import li.cil.oc.core.util.TaskScheduler;
import li.cil.oc.neoforge.common.EventHandler;

public final class TaskSchedulerImpl implements TaskScheduler {
    @Override
    public void schedule(Runnable task) {
        EventHandler.scheduleServer(task);
    }
}
