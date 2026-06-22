package li.cil.oc.core.impl.util;

import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class EventHandlerDelegate {
    private static EventHandlerDelegate instance;

    public static void setInstance(EventHandlerDelegate inst) {
        instance = inst;
    }

    public static EventHandlerDelegate get() {
        return instance;
    }

    public abstract void scheduleServer(BlockEntity tileEntity);

    public abstract void scheduleServer(Runnable task);

    public abstract void post(Object event);

    public abstract void onRobotStart(Object robot);

    public abstract void onRobotStopped(Object robot);

    public abstract void addKeyboard(li.cil.oc.core.impl.server.component.Keyboard keyboard);
}
