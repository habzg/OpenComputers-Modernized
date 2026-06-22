package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


public class MotionSensor extends TileEntity implements Environment {
    public static BlockEntityType<MotionSensor> TYPE;
    public final li.cil.oc.core.impl.server.component.MotionSensor motionSensor;

    public MotionSensor(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        motionSensor = new li.cil.oc.core.impl.server.component.MotionSensor(this);
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
        return motionSensor.node();
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
    public void dispose() {
        super.dispose();
        if (isServer() && motionSensor != null && motionSensor.node() != null) {
            motionSensor.node().remove();
        }
    }

    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            motionSensor.update();
        }
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public boolean isConnected() {
        return node() != null && node().address() != null && node().network() != null;
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
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        motionSensor.load(nbt, getEffectiveProvider());
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        motionSensor.save(nbt, getEffectiveProvider());
    }
}
