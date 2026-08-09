package li.cil.oc.core.impl.common;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.Log;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class PacketBuilderBase<T extends OutputStream> extends DataOutputStream {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("OpenComputers-PacketBuilder");
    public static boolean isProfilingEnabled = false;

    protected final T stream;

    public PacketBuilderBase(T stream) {
        super(new BufferedOutputStream(stream));
        this.stream = stream;
    }

    @Override
    public void write(int b) {
        try { super.write(b); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        try { super.write(b, off, len); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override
    public void flush() {
        try { super.flush(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override
    public void close() {
        try { super.close(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void logPacket(PacketType packetType, int payloadSize, BlockEntity blockEntity) {
        if (isProfilingEnabled) {
            if (blockEntity != null) {
                log.info("Sending: {} @ {} bytes from ({}, {}, {}).", packetType, payloadSize,
                        blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ());
            } else {
                log.info("Sending: {} @ {} bytes.", packetType, payloadSize);
            }
        }
    }

    public static ByteArrayOutputStream newData(boolean compressed) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(compressed ? 1 : 0);
        return data;
    }

    public void writeBlockEntity(BlockEntity t) {
        try {
            var level = t.getLevel();
            writeInt(level != null ? level.dimension().location().hashCode() : 0);
            writeInt(t.getBlockPos().getX());
            writeInt(t.getBlockPos().getY());
            writeInt(t.getBlockPos().getZ());
            if (isProfilingEnabled) {
                blockEntity = t;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDirection(net.minecraft.core.Direction side) {
        try {
            if (side != null) {
                writeByte(side.get3DDataValue());
            } else {
                writeByte(-1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeEntity(Entity e) {
        try {
            writeInt(e.level().dimension().location().hashCode());
            writeInt(e.getId());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void writeItemStack(ItemStack stack, net.minecraft.core.HolderLookup.Provider provider) {
        try {
            boolean haveStack = stack != null && !stack.isEmpty();
            writeBoolean(haveStack);
            if (haveStack) {
                var result = ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack);
                var tag = result.result().orElse(null);
                CompoundTag nbt = tag instanceof CompoundTag ct ? ct : new CompoundTag();
                writeNBT(nbt);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeNBT(CompoundTag nbt) {
        try {
            boolean haveNbt = nbt != null;
            writeBoolean(haveNbt);
            if (haveNbt) {
                try {
                    NbtIo.write(nbt, this);
                } catch (IOException e) {
                    Log.get().warn("Failed writing NBT.", e);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void writeMedium(int v) {
        try {
            writeByte(v & 0xFF);
            writeByte((v >> 8) & 0xFF);
            writeByte((v >> 16) & 0xFF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writePacketType(PacketType pt) {
        try {
            writeByte(pt.ordinal());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockEntity blockEntity;

    @SuppressWarnings("unused")
    protected abstract byte[] getPayloadBytes() ;

    public abstract void sendToAllPlayers() ;

    public void sendToPlayersNearEntity(Entity e) {
        sendToNearbyPlayers(e.level(), e.getX(), e.getY(), e.getZ(), null);
    }

    public void sendToPlayersNearHost(EnvironmentHost host, Double range) {
        if (host instanceof net.minecraft.world.level.block.entity.BlockEntity te) {
            sendToPlayersNearBlockEntity(te, range);
        } else {
            sendToNearbyPlayers(host.level(), host.xPosition(), host.yPosition(), host.zPosition(), range);
        }
    }

    public abstract void sendToPlayer(Player player) ;

    @SuppressWarnings("unused")
    public abstract void sendToServer() ;

    private static DistanceHelper distanceHelper;

    public static void setDistanceHelper(DistanceHelper helper) {
        distanceHelper = helper;
    }

    private static double distanceSquaredTo(Level level, double x, double y, double z, Player player) {
        final var helper = distanceHelper;
        if (helper != null) {
            return helper.distanceSquared(level, x, y, z, player);
        }
        return player.distanceToSqr(x, y, z);
    }

    @FunctionalInterface
    public interface DistanceHelper {
        double distanceSquared(Level level, double x, double y, double z, Player player);
    }

    public void sendToPlayersNearBlockEntity(BlockEntity t) {
        sendToPlayersNearBlockEntity(t, null);
    }

    public void sendToPlayersNearBlockEntity(BlockEntity t, Double range) {
        if (t.getLevel() instanceof ServerLevel serverLevel) {
            int chunkX = t.getBlockPos().getX() >> 4;
            int chunkZ = t.getBlockPos().getZ() >> 4;
            var server = serverLevel.getServer();
            double maxRange = range != null ? range : (server.getPlayerList().getViewDistance() + 1) * 16.0;
            double maxPacketRangeConfig = OCSettings.get().maxNetworkClientPacketDistance;
            if (maxPacketRangeConfig > 0.0D) {
                maxRange = Math.min(maxRange, maxPacketRangeConfig);
            }
            double maxRangeSq = maxRange * maxRange;
            double cx = t.getBlockPos().getX() + 0.5;
            double cy = t.getBlockPos().getY() + 0.5;
            double cz = t.getBlockPos().getZ() + 0.5;
            for (var player : serverLevel.players()) {
                if (player instanceof ServerPlayer sp) {
                    if (serverLevel.getChunkSource().hasChunk(chunkX, chunkZ)) {
                        if (distanceSquaredTo(serverLevel, cx, cy, cz, sp) <= maxRangeSq) {
                            sendToPlayer(sp);
                        }
                    }
                }
            }
        }
    }

    public void sendToNearbyPlayers(Level world, double x, double y, double z, Double range) {
        if (world instanceof ServerLevel serverLevel) {
            var server = serverLevel.getServer();
            double maxRange = range != null ? range : (server.getPlayerList().getViewDistance() + 1) * 16.0;
            double maxPacketRangeConfig = OCSettings.get().maxNetworkClientPacketDistance;
            if (maxPacketRangeConfig > 0.0D) {
                maxRange = Math.min(maxRange, maxPacketRangeConfig);
            }
            double maxRangeSq = maxRange * maxRange;
            for (var player : serverLevel.players()) {
                if (player instanceof ServerPlayer sp) {
                    if (distanceSquaredTo(world, x, y, z, sp) <= maxRangeSq) {
                        sendToPlayer(sp);
                    }
                }
            }
        }
    }

}
