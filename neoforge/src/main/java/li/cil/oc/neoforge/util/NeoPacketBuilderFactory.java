package li.cil.oc.neoforge.util;

import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.util.PacketBuilderFactory;
import li.cil.oc.neoforge.common.PacketBuilder;

public class NeoPacketBuilderFactory extends PacketBuilderFactory {
    @Override
    public Object createCompressed(PacketType type) {
        return new PacketBuilder.Compressed(type);
    }
}
