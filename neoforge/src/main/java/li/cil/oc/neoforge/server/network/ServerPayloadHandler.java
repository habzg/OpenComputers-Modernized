package li.cil.oc.neoforge.server.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.neoforge.common.network.OCPayload;
import li.cil.oc.neoforge.server.PacketHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handle(OCPayload payload, IPayloadContext context) {
        var player = context.player();
        var buf = Unpooled.wrappedBuffer(payload.data());
        PacketHandler.INSTANCE.onPacketData(buf, player);
    }
}
