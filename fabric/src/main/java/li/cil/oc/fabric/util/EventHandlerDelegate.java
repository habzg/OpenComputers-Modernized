package li.cil.oc.fabric.util;

import java.util.Map;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.fabric.common.blockentity.Robot;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EventHandlerDelegate extends li.cil.oc.core.impl.util.EventHandlerDelegate {
    @Override
    public void scheduleServer(BlockEntity blockEntity) {
        li.cil.oc.fabric.common.EventHandler.scheduleServer(blockEntity);
    }

    @Override
    public void scheduleServer(Runnable runnable) {
        li.cil.oc.fabric.common.EventHandler.scheduleServer(runnable);
    }

    @Override
    public void onRobotStart(Object robot) {
        li.cil.oc.fabric.common.EventHandler.onRobotStart((Robot) robot);
    }

    @Override
    public void onRobotStopped(Object robot) {
        li.cil.oc.fabric.common.EventHandler.onRobotStopped((Robot) robot);
    }

    @Override
    public void addKeyboard(li.cil.oc.core.impl.server.component.Keyboard keyboard) {
        li.cil.oc.fabric.common.EventHandler.addKeyboard(keyboard);
    }

    @Override
    public boolean postRobotMovePre(Agent robot, Direction direction) {
        var event = new RobotMoveEvent.Pre(robot, direction);
        RobotMoveEvent.Pre.EVENT.invoker().onRobotMove(event);
        return !event.isCanceled();
    }

    @Override
    public void postRobotMovePost(Agent robot, Direction direction) {
        RobotMoveEvent.Post.EVENT.invoker().onRobotMove(new RobotMoveEvent.Post(robot, direction));
    }

    @Override
    public boolean postRobotPlaceInAir(Agent robot) {
        var event = new RobotPlaceInAirEvent(robot);
        RobotPlaceInAirEvent.EVENT.invoker().onRobotPlaceInAir(event);
        return event.isAllowed();
    }

    @Override
    public float[] postGeolyzerScan(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        var event = new GeolyzerEvent.Scan(host, options, minX, minY, minZ, maxX, maxY, maxZ);
        GeolyzerEvent.Scan.EVENT.invoker().onGeolyzerScan(event);
        return event.isCanceled() ? null : event.data;
    }

    @Override
    public Map<String, Object> postGeolyzerAnalyze(EnvironmentHost host, Map<?, ?> options, int x, int y, int z) {
        var event = new GeolyzerEvent.Analyze(host, options, new net.minecraft.core.BlockPos(x, y, z));
        GeolyzerEvent.Analyze.EVENT.invoker().onGeolyzerAnalyze(event);
        return event.isCanceled() ? null : event.data;
    }
}
