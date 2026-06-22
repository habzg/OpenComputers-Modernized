package li.cil.oc.neoforge.server;

import li.cil.oc.core.common.PacketType;
import li.cil.oc.neoforge.common.PacketBuilder;

public class CompressedPacketBuilder extends PacketBuilder.Compressed {
    public CompressedPacketBuilder(PacketType packetType) {
        super(packetType);
    }
}
