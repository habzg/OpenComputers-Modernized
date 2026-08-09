package li.cil.oc.neoforge.client;

import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.util.TabletCache;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.client.renderer.PetRenderer;
import li.cil.oc.neoforge.common.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ClientEventHandler {
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onClientTick(ClientTickEvent.Pre e) {
        var localCache = TabletCache.forSide(true);
        if (localCache != null) localCache.cleanUp();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && Minecraft.getInstance().isPaused()) {
            var serverCache = TabletCache.get();
            if (serverCache != null) serverCache.keepAlive();
            if (localCache != null) localCache.keepAlive();
        }
        java.lang.Runnable[] adds;
        synchronized (EventHandler.pendingClient) {
            adds = EventHandler.pendingClient.toArray(new java.lang.Runnable[0]);
            EventHandler.pendingClient.clear();
        }
        for (java.lang.Runnable callback : adds) {
            try {
                callback.run();
            } catch (Throwable t) {
                OpenComputers.log().warn("Error in scheduled tick action.", t);
            }
        }
        li.cil.oc.core.impl.util.ClientTickScheduler.runPending();
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void clientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        PetRenderer.isInitialized = false;
        PetRenderer.hidden.clear();
        LootManager.disksForClient.clear();
        for (ItemStack[] entry : LootManager.globalDisks) {
            LootManager.disksForClient.add(entry[0].copy());
        }
        LootManager.disksForCyclingClient.clear();
        LootManager.pendingDiskSync = true;

        Sound.startLoop(null, "computer_running", 0f, 0);
        EventHandler.scheduleServer(() -> Sound.stopLoop(null));
    }
}
