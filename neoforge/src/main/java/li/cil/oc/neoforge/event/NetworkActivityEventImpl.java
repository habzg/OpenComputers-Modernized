package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class NetworkActivityEventImpl extends Event implements NetworkActivityEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final Level level;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final BlockEntity tileEntity;
    protected final CompoundTag data;

    public NetworkActivityEventImpl(BlockEntity tileEntity, CompoundTag data) {
        this.level = tileEntity.getLevel();
        this.x = tileEntity.getBlockPos().getX() + 0.5;
        this.y = tileEntity.getBlockPos().getY() + 0.5;
        this.z = tileEntity.getBlockPos().getZ() + 0.5;
        this.tileEntity = tileEntity;
        this.data = data;
    }

    public NetworkActivityEventImpl(Level world, double x, double y, double z, CompoundTag data) {
        this.level = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileEntity = null;
        this.data = data;
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

    public static class Server extends NetworkActivityEventImpl implements NetworkActivityEvent.Server {
        private final Node node;

        public Server(BlockEntity tileEntity, Node node) {
            super(tileEntity, new CompoundTag());
            this.node = node;
        }

        public Server(Level world, double x, double y, double z, Node node) {
            super(world, x, y, z, new CompoundTag());
            this.node = node;
        }

        @Override
        public Node node() {
            return node;
        }
    }

    public static class Client extends NetworkActivityEventImpl implements NetworkActivityEvent.Client {
        public Client(BlockEntity tileEntity, CompoundTag data) {
            super(tileEntity, data);
        }

        public Client(Level world, double x, double y, double z, CompoundTag data) {
            super(world, x, y, z, data);
        }
    }
}
