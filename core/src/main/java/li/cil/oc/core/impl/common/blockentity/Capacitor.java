package li.cil.oc.core.impl.common.blockentity;

import java.util.Map;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.traits.Environment;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Capacitor extends BlockEntity implements Environment, DeviceInfo {
    public static BlockEntityType<Capacitor> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withConnector(maxCapacity())
            .create();
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Battery",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "CapBank3x",
            DeviceInfo.DeviceAttribute.Capacity, String.valueOf(maxCapacity())
    );

    protected Capacitor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Capacitor(BlockPos pos, BlockState state) {
        this(TYPE, pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public void initialize() {
        if (isServer()) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
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
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        if (node.host() == this) {
            node.save(nbt, getEffectiveProvider());
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        if (node.host() == this) {
            node.load(nbt, getEffectiveProvider());
        }
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) recomputeCapacity(true);
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message ignoredMessage) {
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) {
            for (var coord : indirectNeighbors()) {
                var nx = coord[0];
                var ny = coord[1];
                var nz = coord[2];
                var capPos = new BlockPos(nx, ny, nz);
                if (level().hasChunk(capPos.getX() >> 4, capPos.getZ() >> 4)) {
                    var te = level().getBlockEntity(capPos);
                    if (te instanceof Capacitor cap) cap.recomputeCapacity(false);
                }
            }
        }
    }

    public void recomputeCapacity(boolean updateSecondGradeNeighbors) {
        var bonus = 0;
        for (var side : Direction.values()) {
            var nx = worldPosition.getX() + side.getStepX();
            var ny = worldPosition.getY() + side.getStepY();
            var nz = worldPosition.getZ() + side.getStepZ();
            var capPos = new BlockPos(nx, ny, nz);
            if (level().hasChunk(capPos.getX() >> 4, capPos.getZ() >> 4)) {
                var te = level().getBlockEntity(capPos);
                if (te instanceof Capacitor) bonus++;
            }
        }
        var indirectBonus = 0;
        for (var coord : indirectNeighbors()) {
            var nx = coord[0];
            var ny = coord[1];
            var nz = coord[2];
            var capPos = new BlockPos(nx, ny, nz);
            if (level().hasChunk(capPos.getX() >> 4, capPos.getZ() >> 4)) {
                var te = level().getBlockEntity(new BlockPos(nx, ny, nz));
                if (te instanceof Capacitor cap) {
                    if (updateSecondGradeNeighbors) cap.recomputeCapacity(false);
                    indirectBonus++;
                }
            }
        }
        var buffer = OCSettings.get().bufferCapacitor
                + OCSettings.get().bufferCapacitorAdjacencyBonus * bonus
                + OCSettings.get().bufferCapacitorAdjacencyBonus / 2 * indirectBonus;
        ((li.cil.oc.api.network.Connector) node).setLocalBufferSize(buffer);
    }

    private int[][] indirectNeighbors() {
        var result = new int[Direction.values().length][3];
        var i = 0;
        for (var side : Direction.values()) {
            result[i][0] = worldPosition.getX() + side.getStepX() * 2;
            result[i][1] = worldPosition.getY() + side.getStepY() * 2;
            result[i][2] = worldPosition.getZ() + side.getStepZ() * 2;
            i++;
        }
        return result;
    }

    protected double maxCapacity() {
        return OCSettings.get().bufferCapacitorAdjacencyBonus + OCSettings.get().bufferCapacitorAdjacencyBonus * 9;
    }
}
