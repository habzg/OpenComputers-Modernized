package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class FileSystemAccessEventImpl extends Event implements FileSystemAccessEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final String sound;
    protected final Level level;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final BlockEntity tileEntity;
    protected final CompoundTag data;

    public FileSystemAccessEventImpl(String sound, BlockEntity tileEntity, CompoundTag data) {
        this.sound = sound;
        this.level = tileEntity.getLevel();
        this.x = tileEntity.getBlockPos().getX() + 0.5;
        this.y = tileEntity.getBlockPos().getY() + 0.5;
        this.z = tileEntity.getBlockPos().getZ() + 0.5;
        this.tileEntity = tileEntity;
        this.data = data;
    }

    public FileSystemAccessEventImpl(String sound, Level world, double x, double y, double z, CompoundTag data) {
        this.sound = sound;
        this.level = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileEntity = null;
        this.data = data;
    }

    @Override
    public String sound() {
        return sound;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }

    @Override
    public BlockEntity tileEntity() {
        return tileEntity;
    }

    @Override
    public CompoundTag data() {
        return data;
    }

    public static class Server extends FileSystemAccessEventImpl implements FileSystemAccessEvent.Server {
        private final Node node;

        public Server(String sound, BlockEntity tileEntity, Node node) {
            super(sound, tileEntity, new CompoundTag());
            this.node = node;
        }

        public Server(String sound, Level world, double x, double y, double z, Node node) {
            super(sound, world, x, y, z, new CompoundTag());
            this.node = node;
        }

        @Override
        public Node node() {
            return node;
        }
    }

    public static class Client extends FileSystemAccessEventImpl implements FileSystemAccessEvent.Client {
        public Client(String sound, BlockEntity tileEntity, CompoundTag data) {
            super(sound, tileEntity, data);
        }

        public Client(String sound, Level world, double x, double y, double z, CompoundTag data) {
            super(sound, world, x, y, z, data);
        }
    }
}
