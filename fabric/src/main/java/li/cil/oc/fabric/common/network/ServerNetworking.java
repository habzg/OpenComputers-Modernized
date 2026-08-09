package li.cil.oc.fabric.common.network;

import li.cil.oc.fabric.server.ServerPacketHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class ServerNetworking {
    private ServerNetworking() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(OCPayload.TYPE, OCPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OCPayload.TYPE, OCPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OCPayload.TYPE, (payload, context) -> {
            var player = context.player();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                    io.netty.buffer.Unpooled.wrappedBuffer(payload.data()),
                    player.registryAccess());
            var server = player.getServer();
            if (server != null) {
                server.execute(() -> ServerPacketHandler.INSTANCE.onPacketData(buf, player));
            }
        });
    }
}
