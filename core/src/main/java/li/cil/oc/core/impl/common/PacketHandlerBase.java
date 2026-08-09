package li.cil.oc.core.impl.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PacketHandlerBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketHandlerBase.class);

    protected PacketParser createParser(InputStream stream, Player player) {
        return new PacketParser(stream, player);
    }

    public void onPacketData(ByteBuf data, Player player) {
        InputStream stream = null;
        try {
            stream = new ByteBufInputStream(data);
            if (stream.read() != 0) stream = new InflaterInputStream(stream);
            dispatch(createParser(stream, player));
        } catch (Throwable e) {
            LOGGER.warn("Received a badly formatted packet.", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
            if (data != null && data.refCnt() > 0) {
                data.release();
            }
        }

        if (player instanceof ServerPlayer mp) {
            mp.resetLastActionTime();
        }
    }

    protected abstract Level world(Player player, int ignoredDimension);

    protected abstract void dispatch(PacketParser p);
}
