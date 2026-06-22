package li.cil.oc.neoforge.common;

import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.neoforge.common.network.OCPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SimplePacketBuilder extends PacketBuilderBase<ByteArrayOutputStream> {
    public final PacketType packetType;

    public SimplePacketBuilder(PacketType packetType) {
        super(newData(false));
        this.packetType = packetType;
        try {
            writeByte(packetType.ordinal());
        } catch (IOException e) {
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
