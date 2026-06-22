package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.neoforge.common.EventHandler;
import li.cil.oc.neoforge.common.tileentity.Robot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

public class NeoEventHandlerDelegate extends EventHandlerDelegate {
    @Override
    public void scheduleServer(BlockEntity tileEntity) {
        EventHandler.scheduleServer(tileEntity);
    }

    @Override
    public void scheduleServer(Runnable task) {
        EventHandler.scheduleServer(task);
    }

    @Override
    public void post(Object event) {
        if (event instanceof li.cil.oc.api.event.Event ocEvent) {
            li.cil.oc.api.event.OCEventBus.post(ocEvent);
        } else if (event instanceof Event neoEvent) {
            NeoForge.EVENT_BUS.post(neoEvent);
        }
    }

    @Override
    public void onRobotStart(Object robot) {
        EventHandler.onRobotStart((Robot) robot);
    }

    @Override
    public void onRobotStopped(Object robot) {
        EventHandler.onRobotStopped((Robot) robot);
    }

    @Override
    public void addKeyboard(li.cil.oc.core.impl.server.component.Keyboard keyboard) {
        EventHandler.addKeyboard(keyboard);
    }
}
