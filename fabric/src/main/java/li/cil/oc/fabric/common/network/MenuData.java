package li.cil.oc.fabric.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MenuData(int guiType, int x, int y, int z, String address) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuData> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        MenuData::guiType,
        ByteBufCodecs.VAR_INT,
        MenuData::x,
        ByteBufCodecs.VAR_INT,
        MenuData::y,
        ByteBufCodecs.VAR_INT,
        MenuData::z,
        ByteBufCodecs.STRING_UTF8,
        MenuData::address,
        MenuData::new
    );
}
