package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.PowerBalancer;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


public class PowerDistributor extends TileEntity implements Environment, PowerBalancer, NotAnalyzable {

    public static BlockEntityType<PowerDistributor> TYPE;
    public final Node[] nodes;
    private double _globalBuffer = 0;
    private double _globalBufferSize = 0;
    private double lastSentRatio = -1.0;
    private int ticksUntilSync = 0;

    public PowerDistributor(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        nodes = new Node[6];
        for (int i = 0; i < 6; i++) {
            nodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.None)
                    .withConnector(Settings.get().bufferDistributor)
                    .create();
        }
    }

    @Override
    public Node node() {
        return null;
    }

    @Override
    public Level level() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double yPosition() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double zPosition() {
        return worldPosition.getZ() + 0.5;
    }

    @Override
    public void markChanged() {
    }

    @Override
    public boolean isConnected() {
        for (var n : nodes) if (n != null && n.address() != null && n.network() != null) return true;
        return false;
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        var level = getLevel();
        if (level == null || level.isClientSide) return;
        if (!isConnected()) return;
        if (level.getGameTime() % Settings.get().tickFrequency != 0) return;

        var connectors = new Connector[6];
        for (int i = 0; i < 6; i++) {
            if (nodes[i] instanceof Connector c) connectors[i] = c;
        }

        var lock0 = networkLock(connectors[0]);
        var lock1 = networkLock(connectors[1]);
        var lock2 = networkLock(connectors[2]);
        var lock3 = networkLock(connectors[3]);
        var lock4 = networkLock(connectors[4]);
        var lock5 = networkLock(connectors[5]);

        synchronized (lock0) {
            synchronized (lock1) {
                synchronized (lock2) {
                    synchronized (lock3) {
                        synchronized (lock4) {
                            synchronized (lock5) {
                                double sumBuffer = 0, sumSize = 0;
                                var primary = new java.util.ArrayList<Connector>();
                                for (int i = 0; i < 6; i++) {
                                    var c = connectors[i];
                                    if (c != null && isPrimary(connectors, c)) {
                                        primary.add(c);
                                        sumBuffer += c.globalBuffer();
                                        sumSize += c.globalBufferSize();
                                    }
                                }
                                if (sumSize > 0) {
                                    double ratio = sumBuffer / sumSize;
                                    for (var c : primary) {
                                        c.changeBuffer(c.globalBufferSize() * ratio - c.globalBuffer());
                                    }
                                }
                                _globalBuffer = sumBuffer;
                                _globalBufferSize = sumSize;
                            }
                        }
                    }
                }
            }
        }
        updatePowerInformation();
    }

    private Object networkLock(Connector c) {
        return (c != null && c.network() != null) ? c.network() : this;
    }

    private boolean isPrimary(Connector[] connectors, Connector c) {
        for (var other : connectors) {
            if (other != null && other.network() == c.network()) return other == c;
        }
        return true;
    }

    @Override
    public void updatePowerInformation() {
        double ratio = _globalBufferSize > 0 ? _globalBuffer / _globalBufferSize : 0;
        if (shouldSync(ratio) || hasChangedSignificantly(ratio)) {
            lastSentRatio = ratio;
            PacketSender.sendPowerState(this, _globalBuffer, _globalBufferSize);
        }
    }

    private boolean hasChangedSignificantly(double ratio) {
        return lastSentRatio < 0 || Math.abs(lastSentRatio - ratio) > 0.05;
    }

    private boolean shouldSync(double ratio) {
        ticksUntilSync--;
        if (ticksUntilSync <= 0) {
            ticksUntilSync = Math.max(1, (int) (100 / Settings.get().tickFrequency));
            return lastSentRatio != ratio;
        }
        return false;
    }

    @Override
    public double globalBuffer() {
        return _globalBuffer;
    }

    @Override
    public void globalBuffer(double value) {
        _globalBuffer = value;
    }

    @Override
    public double globalBufferSize() {
        return _globalBufferSize;
    }

    @Override
    public void globalBufferSize(double value) {
        _globalBufferSize = value;
    }

    @Override
    public double globalDemand(Direction side) {
        return 0;
    }

    @Override
    public li.cil.oc.api.network.Node[] onAnalyze(net.minecraft.world.entity.player.Player player, int side, float hitX, float hitY, float hitZ) {
        return null;
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return true;
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        if (side.ordinal() < nodes.length && nodes[side.ordinal()] instanceof li.cil.oc.api.network.Connector c) {
            if (c.tryChangeBuffer(amount)) return amount;
        }
        return 0;
    }

    @Override
    public double globalBuffer(Direction side) {
        if (side.ordinal() < nodes.length && nodes[side.ordinal()] instanceof li.cil.oc.api.network.Connector c) {
            return c.globalBuffer();
        }
        return 0;
    }

    @Override
    public double globalBufferSize(Direction side) {
        if (side.ordinal() < nodes.length && nodes[side.ordinal()] instanceof li.cil.oc.api.network.Connector c) {
            return c.globalBufferSize();
        }
        return 0;
    }

    @Override
    public double energyThroughput() {
        return Settings.get().powerDistributorRate;
    }

    @Override
    public boolean canConnect(Direction side) {
        return true;
    }

    @Override
    public Node sidedNode(Direction side) {
        return nodes[side.ordinal()];
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        var tagList = nbt.getList(Settings.namespace + "connector", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(tagList.size(), nodes.length); i++) {
            nodes[i].load(tagList.getCompound(i), getEffectiveProvider());
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        if (isServer()) {
            var tagList = new ListTag();
            for (var n : nodes) {
                var cnbt = new CompoundTag();
                n.save(cnbt, getEffectiveProvider());
                tagList.add(cnbt);
            }
            nbt.put(Settings.namespace + "connector", tagList);
        }
    }
}
