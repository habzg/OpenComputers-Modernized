package li.cil.oc.fabric.common.network;

import li.cil.oc.core.Tags;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record OCPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OCPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Tags.MOD_ID, "packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OCPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeBytes(payload.data()),
                    buf -> {
                        byte[] data = new byte[buf.readableBytes()];
                        buf.readBytes(data);
                        return new OCPayload(data);
                    }
            );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
