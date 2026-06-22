package li.cil.oc.neoforge.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.block.RobotAfterimage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;


public abstract class PacketHandler {
    public void onPacketData(ByteBuf data, Player player) {
        InputStream stream = null;
        try {
            stream = new ByteBufInputStream(data);
            if (stream.read() != 0) stream = new InflaterInputStream(stream);
            dispatch(new PacketParser(stream, player));
        } catch (Throwable e) {
            OpenComputers.log().warn("Received a badly formatted packet.", e);
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

    @SuppressWarnings("unused")
    protected abstract Level world(Player player, int dimension);

    protected abstract void dispatch(PacketParser p) ;

    public class PacketParser extends DataInputStream {
        public final PacketType packetType;
        public final Player player;

        public PacketParser(InputStream stream, Player player) {
            super(stream);
            this.player = player;
            try {
                this.packetType = PacketType.values()[readByte() & 0xFF];
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T getTileEntity(int dimension, int x, int y, int z, Class<T> clazz) {
            Level world = world(player, dimension);
            if (world != null && world.hasChunk(x >> 4, z >> 4)) {
                var te = world.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
                if (te != null && clazz.isAssignableFrom(te.getClass())) {
                    return clazz.cast(te);
                }
                li.cil.oc.api.detail.ItemInfo info = li.cil.oc.api.Items.get(Constants.BlockName.RobotAfterimage);
                if (info != null && info.block() instanceof RobotAfterimage afterimage) {
                    var proxy = afterimage.findMovingRobot(world, new net.minecraft.core.BlockPos(x, y, z));
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

        public <T> T readTileEntity(Class<T> clazz) {
            try {
                int dimension = readInt();
                int x = readInt();
                int y = readInt();
                int z = readInt();
                return getTileEntity(dimension, x, y, z, clazz);
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

        public net.minecraft.core.Direction readDirection() {
            try {
                byte id = readByte();
                if (id < 0) return null;
                return net.minecraft.core.Direction.from3DDataValue(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public ItemStack readItemStack() {
            try {
                boolean haveStack = readBoolean();
                if (haveStack) {
                    var nbt = readNBT();
                    if (nbt != null) {
                        var result = ItemStack.CODEC.parse(NbtOps.INSTANCE, nbt);
                        return result.result().orElse(ItemStack.EMPTY);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ItemStack.EMPTY;
        }

        public CompoundTag readNBT() {
            try {
                boolean haveNbt = readBoolean();
                if (haveNbt) {
                    return NbtIo.read(this);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        public int readMedium() {
            try {
                int c0 = readUnsignedByte();
                int c1 = readUnsignedByte();
                int c2 = readUnsignedByte();
                return c0 | (c1 << 8) | (c2 << 16);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public PacketType readPacketType() {
            try {
                return PacketType.values()[readByte() & 0xFF];
            } catch (EOFException e) {
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
