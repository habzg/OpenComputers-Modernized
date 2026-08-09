package li.cil.oc.fabric.client;

import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.util.TabletCache;
import li.cil.oc.fabric.client.renderer.PetRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class ClientEventHandler {
    private static ClientLevel currentClientLevel = null;

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            li.cil.oc.fabric.util.Audio.onClientTick();
            var localCache = TabletCache.forSide(true);
            if (localCache != null) localCache.cleanUp();
            var server = client.getSingleplayerServer();
            if (server != null && client.isPaused()) {
                var serverCache = TabletCache.get();
                if (serverCache != null) serverCache.keepAlive();
                if (localCache != null) localCache.keepAlive();
            }
            java.lang.Runnable[] adds;
            synchronized (li.cil.oc.fabric.common.EventHandler.pendingClient) {
                adds = li.cil.oc.fabric.common.EventHandler.pendingClient.toArray(new java.lang.Runnable[0]);
                li.cil.oc.fabric.common.EventHandler.pendingClient.clear();
            }
            for (java.lang.Runnable callback : adds) {
                try {
                    callback.run();
                } catch (Throwable t) {
                    li.cil.oc.fabric.OpenComputers.log().warn("Error in scheduled tick action.", t);
                }
            }
            li.cil.oc.core.impl.util.ClientTickScheduler.runPending();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PetRenderer.isInitialized = false;
            PetRenderer.hidden.clear();
            LootManager.disksForClient.clear();
            for (ItemStack[] entry : LootManager.globalDisks) {
                LootManager.disksForClient.add(entry[0].copy());
            }
            LootManager.disksForCyclingClient.clear();
            LootManager.pendingDiskSync = true;

            Sound.startLoop(null, "computer_running", 0f, 0);
            li.cil.oc.fabric.common.EventHandler.scheduleServer(() -> Sound.stopLoop(null));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.clearAll();
            li.cil.oc.core.impl.client.ClientRobotTracker.INSTANCE.clearAll();
            li.cil.oc.core.impl.common.component.TerminalServer.TerminalServerCache.loaded.clear();
            li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.clear();
            currentClientLevel = null;
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> {
            var oldWorld = currentClientLevel;
            if (oldWorld != null && oldWorld != newWorld) {
                li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.clear(oldWorld);
                li.cil.oc.core.impl.client.ClientRobotTracker.INSTANCE.clear(oldWorld);
                li.cil.oc.core.impl.common.component.TerminalServer.TerminalServerCache.loaded.clear();
                li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.removeIf(t -> {
                    var keep = t.host().level() != oldWorld;
                    if (!keep) {
                        li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.remove(oldWorld, t);
                    }
                    return keep;
                });
            }
            currentClientLevel = newWorld;
        });
    }
}
