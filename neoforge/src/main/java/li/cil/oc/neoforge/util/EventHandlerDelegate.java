package li.cil.oc.neoforge.util;

import java.util.Map;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.neoforge.common.EventHandler;
import li.cil.oc.neoforge.common.blockentity.Robot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;

public class EventHandlerDelegate extends li.cil.oc.core.impl.util.EventHandlerDelegate {
    @Override
    public void scheduleServer(BlockEntity blockEntity) {
        EventHandler.scheduleServer(blockEntity);
    }

    @Override
    public void scheduleServer(Runnable task) {
        EventHandler.scheduleServer(task);
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

    @Override
    public boolean postRobotMovePre(Agent robot, Direction direction) {
        RobotMoveEvent.Pre event = new RobotMoveEvent.Pre(robot, direction);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public void postRobotMovePost(Agent robot, Direction direction) {
        NeoForge.EVENT_BUS.post(new RobotMoveEvent.Post(robot, direction));
    }

    @Override
    public boolean postRobotPlaceInAir(Agent robot) {
        RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(robot);
        NeoForge.EVENT_BUS.post(event);
        return event.isAllowed();
    }

    @Override
    public float[] postGeolyzerScan(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        GeolyzerEvent.Scan event = new GeolyzerEvent.Scan(host, options, minX, minY, minZ, maxX, maxY, maxZ);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? null : event.data;
    }

    @Override
    public Map<String, Object> postGeolyzerAnalyze(EnvironmentHost host, Map<?, ?> options, int x, int y, int z) {
        GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(host, options, new BlockPos(x, y, z));
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? null : event.data;
    }
}
