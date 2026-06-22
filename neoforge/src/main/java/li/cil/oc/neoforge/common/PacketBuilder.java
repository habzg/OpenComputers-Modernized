package li.cil.oc.neoforge.common;

import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.network.OCPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class PacketBuilder {
    private PacketBuilder() {
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
            logPacket(packetType, payload.length, tileEntity);
            PacketDistributor.sendToAllPlayers(new OCPayload(payload));
        }

        @Override
        public void sendToPlayer(Player player) {
            byte[] payload = getPayloadBytes();
            logPacket(packetType, payload.length, tileEntity);
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new OCPayload(payload));
            }
        }

        @Override
        public void sendToServer() {
            byte[] payload = getPayloadBytes();
            logPacket(packetType, payload.length, tileEntity);
            PacketDistributor.sendToServer(new OCPayload(payload));
        }
    }
}
