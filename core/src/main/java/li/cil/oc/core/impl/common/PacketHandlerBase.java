package li.cil.oc.core.impl.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

  protected abstract Level world(Player player, String dimension);

  protected static Level clientLevel(Player player, String dimension) {
    Level level = player.level();
    ResourceLocation key = ResourceLocation.tryParse(dimension);
    return key != null && key.equals(level.dimension().location()) ? level : null;
  }

  protected static Level serverLevel(Player player, String dimension) {
    if (!(player instanceof ServerPlayer sp)) return null;
    ResourceLocation key = ResourceLocation.tryParse(dimension);
    if (key == null) return null;
    return sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, key));
  }

  protected abstract void dispatch(PacketParser p);
}
