package li.cil.oc.fabric.common.network;

import java.io.IOException;
import java.io.InputStream;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.PacketHandlerBase;
import li.cil.oc.fabric.common.block.RobotAfterimage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class CommonPacketHandler extends PacketHandlerBase {
    @Override
    protected PacketParser createParser(InputStream stream, Player player) {
        return new PacketParser(stream, player);
    }

    @Override
    protected void dispatch(li.cil.oc.core.impl.common.PacketParser p) {
        if (p instanceof PacketParser pp) {
            dispatch(pp);
        }
    }

    protected abstract void dispatch(PacketParser p);

    public class PacketParser extends li.cil.oc.core.impl.common.PacketParser {
        public PacketParser(InputStream stream, Player player) {
            super(stream, player);
        }

        public <T> T getBlockEntity(int dimension, int x, int y, int z, Class<T> clazz) {
            Level world = world(player, dimension);
            if (world != null && world.hasChunk(x >> 4, z >> 4)) {
                var te = world.getBlockEntity(new BlockPos(x, y, z));
                if (te != null && clazz.isAssignableFrom(te.getClass())) {
                    return clazz.cast(te);
                }
                li.cil.oc.api.detail.ItemInfo info = li.cil.oc.api.Items.get(Constants.BlockName.RobotAfterimage);
                if (info != null && info.block() instanceof RobotAfterimage afterimage) {
                    var proxy = afterimage.findMovingRobot(world, new BlockPos(x, y, z));
                    if (proxy != null && clazz.isAssignableFrom(proxy.getClass())) {
                        return clazz.cast(proxy);
                    }
                }
            }
            return null;
        }

        public <T> T getEntity(int dimension, int id, Class<T> clazz) {
            Level world = world(player, dimension);
            if (world != null) {
                var e = world.getEntity(id);
                if (e != null && clazz.isAssignableFrom(e.getClass())) {
                    return clazz.cast(e);
                }
            }
            return null;
        }

        public <T> T readBlockEntity(Class<T> clazz) {
            try {
                int dimension = readInt();
                int x = readInt();
                int y = readInt();
                int z = readInt();
                return getBlockEntity(dimension, x, y, z, clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T readEntity(Class<T> clazz) {
            try {
                int dimension = readInt();
                int id = readInt();
                return getEntity(dimension, id, clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
