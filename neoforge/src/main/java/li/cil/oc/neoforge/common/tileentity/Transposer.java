package li.cil.oc.neoforge.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.TransposerData;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;


public class Transposer extends TileEntity implements Environment {
    public final TransposerData info = new TransposerData();
    public final li.cil.oc.neoforge.server.component.Transposer.Block transposer;
    public long lastOperation = 0;

    @Override
    public long getLastOperation() {
        return lastOperation;
    }

    public Transposer(BlockPos pos, BlockState state) {
        super(li.cil.oc.neoforge.common.init.TileEntities.TRANSPOSER.get(), pos, state);
        transposer = new li.cil.oc.neoforge.server.component.Transposer.Block(this);
    }

    @Override
    public Node node() {
        return transposer.node();
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
        return node() != null && node().address() != null && node().network() != null;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer() && transposer != null && transposer.node() != null) transposer.node().remove();
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
        var provider = getEffectiveProvider();
        if (provider != null) {
            info.load(nbt.getCompound(Settings.namespace + "info"), provider);
            transposer.load(nbt, provider);
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        var provider = getEffectiveProvider();
        var infoTag = new net.minecraft.nbt.CompoundTag();
        info.save(infoTag, provider);
        nbt.put(Settings.namespace + "info", infoTag);
        transposer.save(nbt, provider);
    }
}
