package li.cil.oc.neoforge.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.component.TerminalServer;
import li.cil.oc.core.impl.server.component.Keyboard;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.PlayerUtils;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.impl.util.TabletCache;
import li.cil.oc.core.server.machine.Callbacks;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.blockentity.Robot;
import li.cil.oc.neoforge.server.machine.Machine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class EventHandler {
    private static long serverTicks = 0L;
    private static final PriorityQueue<TimedTask> pendingServerTimed = new PriorityQueue<>(Comparator.comparingLong(t -> t.tick));
    private static final List<Runnable> pendingServer = Collections.synchronizedList(new ArrayList<>());
    public static final List<Runnable> pendingClient = Collections.synchronizedList(new ArrayList<>());

    private record TimedTask(long tick, Runnable task) {
    }

    private static final Set<Robot> runningRobots = ConcurrentHashMap.newKeySet();
    private static final Set<Keyboard> keyboards = ConcurrentHashMap.newKeySet();
    private static final Set<Machine> machines = ConcurrentHashMap.newKeySet();

    public static void onRobotStart(Robot robot) {
        runningRobots.add(robot);
    }

    public static void onRobotStopped(Robot robot) {
        runningRobots.remove(robot);
    }

    public static void scheduleClose(Machine machine) {
        machines.add(machine);
    }

    public static void unscheduleClose(Machine machine) {
        machines.remove(machine);
    }

    public static void addKeyboard(Keyboard keyboard) {
        keyboards.add(keyboard);
    }

    public static void scheduleServer(BlockEntity blockEntity) {
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

    public static void scheduleServer(Runnable f, int delay) {
        synchronized (pendingServerTimed) {
            pendingServerTimed.add(new TimedTask(serverTicks + Math.max(delay, 0), f));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onServerTick(ServerTickEvent.Pre e) {
        Runnable[] adds;
        synchronized (pendingServer) {
            adds = pendingServer.toArray(new Runnable[0]);
            pendingServer.clear();
        }
        for (Runnable callback : adds) {
            try {
                callback.run();
            } catch (Throwable t) {
                OpenComputers.log().warn("Error in scheduled tick action.", t);
            }
        }

        serverTicks++;
        synchronized (pendingServerTimed) {
            while (!pendingServerTimed.isEmpty() && pendingServerTimed.peek().tick < serverTicks) {
                var timed = pendingServerTimed.poll();
                try {
                    timed.task.run();
                } catch (Throwable t) {
                    OpenComputers.log().warn("Error in scheduled timed tick action.", t);
                }
            }
        }

        List<Robot> invalid = new ArrayList<>();
        for (Robot robot : runningRobots) {
            if (robot.isRemoved()) invalid.add(robot);
            else if (robot.level() != null) robot.machine().update();
        }
        invalid.forEach(runningRobots::remove);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onServerTick(ServerTickEvent.Post e) {
        var cache = TabletCache.get();
        if (cache != null) cache.cleanUp();
        List<Machine> closed = new ArrayList<>();
        for (Machine machine : machines) {
            if (machine.tryClose()) {
                closed.add(machine);
                var hostPos = BlockPosition.apply(machine.host()).toBlockPos();
                if (machine.host().level() == null || !machine.host().level().hasChunk(hostPos.getX() >> 4, hostPos.getZ() >> 4)) {
                    if (machine.node() != null) machine.node().remove();
                }
            }
        }
        closed.forEach(machines::remove);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (SideTracker.isServer() && e.getEntity() instanceof ServerPlayer player && !(e.getEntity() instanceof FakePlayer)) {
            if (!LuaStateFactory.isAvailable() && !LuaStateFactory.luajRequested()) {
                player.sendSystemMessage(Component.literal(net.minecraft.ChatFormatting.GREEN + "OpenComputers" + net.minecraft.ChatFormatting.RESET + ": ").append(Component.translatable("gui.opencomputers.chat.warningluafallback")));
            }
            PacketSender.sendPetVisibility(null, player);
            PacketSender.sendLootDisks(player);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        var te = e.getLevel().getBlockEntity(e.getPos());
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Case c) {
            if (c.isCreative() && (!e.getPlayer().getAbilities().instabuild || !c.canInteract(e.getPlayer().getScoreboardName()))) {
                e.setCanceled(true);
            }
        } else if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy) {
            var robot = proxy.robot;
            if (robot.isCreative() && (!e.getPlayer().getAbilities().instabuild || !robot.canInteract(e.getPlayer().getScoreboardName()))) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        for (Keyboard kb : keyboards) kb.releasePressedKeys(e.getEntity());
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        for (Keyboard kb : keyboards) kb.releasePressedKeys(e.getEntity());
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        for (Keyboard kb : keyboards) kb.releasePressedKeys(e.getEntity());
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onEntityJoinWorld(EntityJoinLevelEvent e) {
        if (OCSettings.get().giveManualToNewPlayers && !e.getLevel().isClientSide && e.getEntity() instanceof Player player && !(e.getEntity() instanceof FakePlayer)) {
            var persistedData = PlayerUtils.persistedData(player);
            if (!persistedData.getBoolean(OCSettings.namespace + "receivedManual")) {
                persistedData.putBoolean(OCSettings.namespace + "receivedManual", true);
                player.getInventory().add(API.items.get(Constants.ItemName.Manual).createItemStack(1));
            }
        }
    }

    public static boolean isItTime() {
        var now = Calendar.getInstance();
        return now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) == 1;
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onWorldUnload(LevelEvent.Unload e) {
        var levelAccessor = e.getLevel();
        if (!(levelAccessor instanceof net.minecraft.world.level.Level worldLevel)) return;
        var cache = TabletCache.forSide(worldLevel.isClientSide());
        if (cache != null) cache.clear(worldLevel);
        synchronized (EventHandler.class) {
            if (!worldLevel.isClientSide()) {
                var serverLevel = (net.minecraft.server.level.ServerLevel) worldLevel;
                for (var entity : serverLevel.getEntities().getAll()) {
                    if (entity instanceof MachineHost host) host.machine().stop();
                }
                Callbacks.clear();
                li.cil.oc.core.impl.server.ServerRobotRegistry.INSTANCE.clear(worldLevel);
            } else {
                li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.clear(worldLevel);
                li.cil.oc.core.impl.client.ClientRobotTracker.INSTANCE.clear(worldLevel);
                TerminalServer.TerminalServerCache.loaded.clear();
                li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.removeIf(t -> {
                    var keep = t.host().level() != worldLevel;
                    if (!keep) {
                        li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.remove(worldLevel, t);
                    }
                    return keep;
                });
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onChunkUnload(ChunkEvent.Unload e) {
        if (!e.getLevel().isClientSide()) {
            var chunkPos = e.getChunk().getPos();
            var chunkMin = new net.minecraft.core.BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
            var chunkMax = new net.minecraft.core.BlockPos(chunkPos.getMaxBlockX(), 255, chunkPos.getMaxBlockZ());
            var aabb = new net.minecraft.world.phys.AABB(chunkMin.getX(), chunkMin.getY(), chunkMin.getZ(), chunkMax.getX(), chunkMax.getY(), chunkMax.getZ());
            for (var entity : e.getLevel().getEntities(null, aabb)) {
                if (entity instanceof MachineHost host) {
                    if (host.machine() instanceof Machine machine) {
                        scheduleClose(machine);
                    }
                } else if (entity instanceof Rack rack) {
                    for (int i = 0; i < rack.getContainerSize(); i++) {
                        if (rack.getMountable(i) instanceof Server server && server.machine() != null) {
                            server.machine().stop();
                        }
                    }
                }
            }
            var chunk = e.getChunk();
            if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
                for (var be : levelChunk.getBlockEntities().values()) {
                    if (be instanceof li.cil.oc.core.impl.common.blockentity.traits.BlockEntity te) {
                        try {
                            te.dispose();
                        } catch (Throwable t) {
                            OpenComputers.log().warn("Failed disposing block entity on chunk unload.", t);
                        }
                    }
                    if (be instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) {
                        if (screen.node != null) {
                            screen.node.remove();
                        }
                    }
                }
            }
        } else {
            var chunk = e.getChunk();
            if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
                for (var be : levelChunk.getBlockEntities().values()) {
                    if (be instanceof Rack rack) {
                        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                            if (rack.getMountable(slot) instanceof li.cil.oc.core.impl.common.component.TerminalServer terminal) {
                                var buffer = terminal.bufferIfLoaded();
                                if (buffer != null) {
                                    li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.remove(rack.level(), buffer);
                                    if (buffer instanceof li.cil.oc.core.impl.common.component.TextBuffer concreteBuffer) {
                                        li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.remove(concreteBuffer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
