package li.cil.oc.core.impl.util;

import net.minecraft.server.MinecraftServer;

import java.util.function.Supplier;

public final class SideTracker {
    private static boolean isDedicatedServer;
    private static Supplier<MinecraftServer> currentServer = () -> null;

    private SideTracker() {
    }

    public static void setDedicatedServer(boolean value) {
        isDedicatedServer = value;
    }

    public static void setCurrentServer(Supplier<MinecraftServer> server) {
        currentServer = server;
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer.get();
    }

    public static boolean isServer() {
        if (isDedicatedServer) return true;
        MinecraftServer server = currentServer.get();
        return server != null && Thread.currentThread() == server.getRunningThread();
    }

    public static boolean isClient() {
        if (isDedicatedServer) return false;
        MinecraftServer server = currentServer.get();
        return server == null || Thread.currentThread() != server.getRunningThread();
    }
}
