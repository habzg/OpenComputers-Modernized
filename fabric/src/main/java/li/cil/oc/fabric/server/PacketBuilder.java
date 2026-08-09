package li.cil.oc.fabric.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.common.network.OCPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketBuilder extends PacketBuilderBase<ByteArrayOutputStream> {
    public static MinecraftServer SERVER;

    public final PacketType packetType;

    public PacketBuilder(PacketType packetType) {
        super(newData(false));
        this.packetType = packetType;
        try {
            writeByte(packetType.ordinal());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected byte[] getPayloadBytes() {
        flush();
        return stream.toByteArray();
    }

    @Override
    public void sendToAllPlayers() {
        byte[] payload = getPayloadBytes();
        logPacket(packetType, payload.length, blockEntity);
        if (SERVER != null) {
            var packet = new OCPayload(payload);
            for (var player : SERVER.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    @Override
    public void sendToPlayer(Player player) {
        byte[] payload = getPayloadBytes();
        logPacket(packetType, payload.length, blockEntity);
        if (player instanceof ServerPlayer sp) {
            ServerPlayNetworking.send(sp, new OCPayload(payload));
        }
    }

    @Override
    public void sendToServer() {
        byte[] payload = getPayloadBytes();
        logPacket(packetType, payload.length, blockEntity);
        ClientPlayNetworking.send(new OCPayload(payload));
    }

    public static class Compressed extends PacketBuilderBase<DeflaterOutputStream> {
        public final PacketType packetType;
        private final ByteArrayOutputStream data;

        public Compressed(PacketType packetType) {
            this(packetType, newData(true));
        }

        private Compressed(PacketType packetType, ByteArrayOutputStream data) {
            super(new DeflaterOutputStream(data, new Deflater(Deflater.BEST_SPEED)));
            this.packetType = packetType;
            this.data = data;
            try {
                writeByte(packetType.ordinal());
            } catch (IOException e) {
                OpenComputers.log().warn("Failed writing packet type header.", e);
            }
        }

        @Override
        protected byte[] getPayloadBytes() {
            flush();
            try {
                stream.finish();
            } catch (IOException e) {
                OpenComputers.log().warn("Failed finishing compression.", e);
            }
            return data.toByteArray();
        }

        @Override
        public void sendToAllPlayers() {
            byte[] payload = getPayloadBytes();
            logPacket(packetType, payload.length, blockEntity);
            var packet = new OCPayload(payload);
            if (SERVER != null) {
                for (var player : SERVER.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, packet);
                }
            }
        }

        @Override
        public void sendToPlayer(Player player) {
            byte[] payload = getPayloadBytes();
            logPacket(packetType, payload.length, blockEntity);
            if (player instanceof ServerPlayer sp) {
                ServerPlayNetworking.send(sp, new OCPayload(payload));
            }
        }

        @Override
        public void sendToServer() {
            byte[] payload = getPayloadBytes();
            logPacket(packetType, payload.length, blockEntity);
            ClientPlayNetworking.send(new OCPayload(payload));
        }
    }
}
