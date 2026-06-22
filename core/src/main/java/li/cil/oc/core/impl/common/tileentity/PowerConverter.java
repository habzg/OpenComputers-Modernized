package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class PowerConverter extends TileEntity implements PowerAcceptor, Environment, NotAnalyzable, DeviceInfo {

    public static BlockEntityType<PowerConverter> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.None)
            .withConnector(Settings.get().bufferConverter)
            .create();
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Power converter",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Transgizer-PX5",
            DeviceInfo.DeviceAttribute.Capacity, String.valueOf(energyThroughput())
    );

    public PowerConverter(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public Node node() {
        return node;
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
        return node.address() != null && node.network() != null;
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

    public void onNeighborChanged() {
        ae2OnNeighborChanged();
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @SuppressWarnings("SameReturnValue")
    protected boolean hasConnector(Direction ignoredSide) {
        return true;
    }

    protected Connector connector(Direction ignoredSide) {
        return (Connector) node;
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return hasConnector(side);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        var c = connector(side);
        if (c != null && c.tryChangeBuffer(amount)) return amount;
        return 0;
    }

    @Override
    public double globalBuffer(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBuffer() : 0.0;
    }

    @Override
    public double globalBufferSize(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBufferSize() : 0.0;
    }

    @Override
    public double globalDemand(Direction side) {
        var c = connector(side);
        return c != null ? c.globalBufferSize() - c.globalBuffer() : 0.0;
    }

    @Override
    public double energyThroughput() {
        return Settings.get().powerConverterRate;
    }

    @Override
    public Node[] onAnalyze(net.minecraft.world.entity.player.Player player, int side, float hitX, float hitY, float hitZ) {
        return null;
    }
}
