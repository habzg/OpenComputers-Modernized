package li.cil.oc.neoforge.client;

import io.netty.buffer.Unpooled;
import li.cil.oc.neoforge.common.network.OCPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    @SuppressWarnings("unused")
    public static void handle(OCPayload payload, IPayloadContext context) {
        var buf = Unpooled.wrappedBuffer(payload.data());
        ClientPacketHandler.INSTANCE.onPacket(buf);
    }
}
