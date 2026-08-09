package li.cil.oc.fabric.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.Screen;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.server.component.Keyboard;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.PlayerUtils;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.impl.util.TabletCache;
import li.cil.oc.core.server.machine.Callbacks;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class EventHandler {
    private static long serverTicks = 0L;
    private static final PriorityQueue<TimedTask> pendingServerTimed = new PriorityQueue<>(Comparator.comparingLong(t -> t.tick));
    private static final List<Runnable> pendingServer = Collections.synchronizedList(new ArrayList<>());
    public static final List<Runnable> pendingClient = Collections.synchronizedList(new ArrayList<>());
    private static final Set<li.cil.oc.fabric.common.blockentity.Robot> runningRobots = ConcurrentHashMap.newKeySet();

    private record TimedTask(long tick, Runnable task) {
    }

    private static final Set<Keyboard> keyboards = ConcurrentHashMap.newKeySet();

    public static void addKeyboard(Keyboard keyboard) {
        keyboards.add(keyboard);
    }

    public static void onRobotStart(li.cil.oc.fabric.common.blockentity.Robot robot) {
        runningRobots.add(robot);
    }

    public static void onRobotStopped(li.cil.oc.fabric.common.blockentity.Robot robot) {
        runningRobots.remove(robot);
    }

    public static void scheduleServer(net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (SideTracker.isServer()) {
            synchronized (pendingServer) {
                pendingServer.add(() -> Network.joinOrCreateNetwork(blockEntity));
            }
        }
    }

    public static void scheduleServer(Runnable f) {
        synchronized (pendingServer) {
            pendingServer.add(f);
        }
    }

    @SuppressWarnings("unused")
    public static void scheduleServer(Runnable f, int delay) {
        synchronized (pendingServerTimed) {
            pendingServerTimed.add(new TimedTask(serverTicks + Math.max(delay, 0), f));
        }
    }

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            serverTicks++;
            Runnable[] adds;
            synchronized (pendingServer) {
                adds = pendingServer.toArray(new Runnable[0]);
                pendingServer.clear();
            }
            for (Runnable callback : adds) {
                try {
                    callback.run();
                } catch (Throwable t) {
                    li.cil.oc.fabric.OpenComputers.log().warn("Error in scheduled tick action.", t);
                }
            }

            synchronized (pendingServerTimed) {
                while (!pendingServerTimed.isEmpty() && pendingServerTimed.peek().tick < serverTicks) {
                    var timed = pendingServerTimed.poll();
                    try {
                        timed.task.run();
                    } catch (Throwable t) {
                        li.cil.oc.fabric.OpenComputers.log().warn("Error in scheduled timed tick action.", t);
                    }
                }
            }

            List<li.cil.oc.fabric.common.blockentity.Robot> invalid = new ArrayList<>();
            for (var robot : runningRobots) {
                if (robot.isRemoved()) invalid.add(robot);
                else if (robot.level() != null) robot.machine().update();
            }
            invalid.forEach(runningRobots::remove);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var cache = TabletCache.get();
            if (cache != null) cache.cleanUp();
            li.cil.oc.fabric.common.event.BlockChangeHandler.tick();
            List<li.cil.oc.fabric.server.machine.Machine> closed = new ArrayList<>();
            for (var machine : machines) {
                if (machine.tryClose()) {
                    closed.add(machine);
                    var hostPos = BlockPosition.apply(machine.host()).toBlockPos();
                    if (machine.host().level() == null || !machine.host().level().hasChunk(hostPos.getX() >> 4, hostPos.getZ() >> 4)) {
                        if (machine.node() != null) machine.node().remove();
                    }
                }
            }
            closed.forEach(machines::remove);
        });

        ServerPlayerEvents.JOIN.register(player -> {
            if (SideTracker.isServer() && !(player instanceof FakePlayer)) {
                if (!LuaStateFactory.isAvailable() && !LuaStateFactory.luajRequested()) {
                    player.sendSystemMessage(Component.literal(net.minecraft.ChatFormatting.GREEN + "OpenComputers" + net.minecraft.ChatFormatting.RESET + ": ").append(Component.translatable("gui.opencomputers.chat.warningluafallback")));
                }
                PacketSender.sendPetVisibility(null, player);
                PacketSender.sendLootDisks(player);
            }
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            var te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Case c) {
                if (c.isCreative() && (!player.getAbilities().instabuild || !c.canInteract(player.getScoreboardName())))
                    return false;
                return c.canInteract(player.getScoreboardName());
            } else if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy) {
                var robot = proxy.robot;
              return !robot.isCreative() || (player.getAbilities().instabuild && robot.canInteract(player.getScoreboardName()));
            } else if (state.getBlock() instanceof li.cil.oc.fabric.common.block.RobotAfterimage afterimage) {
                var robot = afterimage.findMovingRobot(world, pos);
                if (robot != null && robot.isAnimatingMove() && robot.moveFromX == pos.getX() && robot.moveFromY == pos.getY() && robot.moveFromZ == pos.getZ()) {
                    world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    return false;
                }
            }
            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            for (Keyboard kb : keyboards) kb.releasePressedKeys(newPlayer);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            for (Keyboard kb : keyboards) kb.releasePressedKeys(player);
        });

        ServerPlayerEvents.LEAVE.register(player -> {
            for (Keyboard kb : keyboards) kb.releasePressedKeys(player);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (OCSettings.get().giveManualToNewPlayers && entity instanceof Player player && !(entity instanceof FakePlayer)) {
                var persistedData = PlayerUtils.persistedData(player);
                if (!persistedData.getBoolean(OCSettings.namespace + "receivedManual")) {
                    persistedData.putBoolean(OCSettings.namespace + "receivedManual", true);
                    player.getInventory().add(li.cil.oc.api.API.items.get(Constants.ItemName.Manual).createItemStack(1));
                }
            }
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            var cache = TabletCache.forSide(world.isClientSide());
            if (cache != null) cache.clear(world);
            synchronized (EventHandler.class) {
                if (!world.isClientSide()) {
                    for (var entity : world.getAllEntities()) {
                        if (entity instanceof MachineHost host) host.machine().stop();
                    }
                    Callbacks.clear();
                    li.cil.oc.core.impl.server.ServerRobotRegistry.INSTANCE.clear(world);
                }
            }
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            var chunkPos = chunk.getPos();
            var chunkMin = new net.minecraft.core.BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
            var chunkMax = new net.minecraft.core.BlockPos(chunkPos.getMaxBlockX(), 255, chunkPos.getMaxBlockZ());
            var aabb = new net.minecraft.world.phys.AABB(chunkMin.getX(), chunkMin.getY(), chunkMin.getZ(), chunkMax.getX(), chunkMax.getY(), chunkMax.getZ());
            for (var entity : world.getEntities(null, aabb)) {
                if (entity instanceof MachineHost host) {
                    if (host.machine() instanceof li.cil.oc.fabric.server.machine.Machine machine) {
                        machines.add(machine);
                    }
                } else if (entity instanceof li.cil.oc.api.internal.Rack rack) {
                    for (int i = 0; i < rack.getContainerSize(); i++) {
                        if (rack.getMountable(i) instanceof li.cil.oc.api.internal.Server server2 && server2.machine() != null) {
                            server2.machine().stop();
                        }
                    }
                }
            }
        });

        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof BlockEntity te) {
                scheduleServer(() -> {
                    try {
                        te.initialize();
                    } catch (Throwable t) {
                        li.cil.oc.fabric.OpenComputers.log().warn("Error in block entity initialize", t);
                    }
                });
            }
        });

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof BlockEntity te) {
                try {
                    te.dispose();
                } catch (Throwable t) {
                    li.cil.oc.fabric.OpenComputers.log().warn("Failed disposing block entity on chunk unload.", t);
                }
            }
            if (blockEntity instanceof Screen screen) {
                if (!world.isClientSide && screen.node != null) {
                    screen.node.remove();
                }
            }
        });
    }

    public static void initClient() {
    }

    private static final Set<li.cil.oc.fabric.server.machine.Machine> machines = ConcurrentHashMap.newKeySet();

    public static void scheduleClose(li.cil.oc.fabric.server.machine.Machine machine) {
        machines.add(machine);
    }

    public static void unscheduleClose(li.cil.oc.fabric.server.machine.Machine machine) {
        machines.remove(machine);
    }

    public static boolean isItTime() {
        var now = Calendar.getInstance();
        return now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) == 1;
    }
}
