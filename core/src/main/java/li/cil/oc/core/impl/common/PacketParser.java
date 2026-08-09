package li.cil.oc.core.impl.common;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import li.cil.oc.core.common.PacketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
                return NbtIo.read(this, NbtAccounter.create(0x200000L));
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
