package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.server.component.RedstoneSignaller;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class Redstone extends TileEntity implements Environment, BundledRedstoneAware {
    public static BlockEntityType<Redstone> TYPE;
    public final RedstoneSignaller instance;
    public final Node node;
    public final Node dummyNode;
    private boolean _isOutputEnabled;
    private boolean shouldUpdateInput;
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
    private final int[] _output = new int[6];
    private final int[][] _bundledInput = new int[6][16];
    private final int[][] _rednetInput = new int[6][16];
    private final int[][] _bundledOutput = new int[6][16];

    public Redstone(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        if (BundledRedstone.isAvailable()) instance = new li.cil.oc.core.impl.server.component.Redstone.Bundled(this);
        else instance = new li.cil.oc.core.impl.server.component.Redstone.Vanilla(this);
        instance.wakeNeighborsOnly = false;
        var n = instance.node();
        if (n instanceof li.cil.oc.api.network.Component component) {
            component.setVisibility(Visibility.Network);
            _isOutputEnabled = true;
            dummyNode = li.cil.oc.api.Network.newNode(this, Visibility.None).create();
        } else {
            _isOutputEnabled = false;
            dummyNode = null;
        }
        node = n;
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
        return node != null && node.address() != null && node.network() != null;
    }

    @Override
    public void initialize() {
        EventHandlerDelegate.get().scheduleServer(this);
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
    public void checkRedstoneInputChanged() {
        if (isServer()) {
            shouldUpdateInput = true;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        shouldUpdateInput = true;
    }

    @Override
    public void updateRedstoneInput(Direction side) {
        setInput(side, BundledRedstone.computeInput(BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), side));
        setBundledInput(side, BundledRedstone.computeBundledInput(position(), side));
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            if (shouldUpdateInput) {
                shouldUpdateInput = false;
                for (var side : Direction.values()) {
                    updateRedstoneInput(side);
                }
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        var provider = getEffectiveProvider();
        if (provider != null)
            instance.load(nbt.getCompound(Settings.namespace + "redstone"), provider);
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "redstone", t -> instance.save(t, getEffectiveProvider()));
    }

    @Override
    public boolean isOutputEnabled() {
        return _isOutputEnabled;
    }

    @Override
    public void setOutputEnabled(boolean value) {
        _isOutputEnabled = value;
    }

    @Override
    public int[] input() {
        return _input;
    }

    @Override
    public int getInput(Direction side) {
        return Math.max(_input[side.ordinal()], 0);
    }

    @Override
    public void setInput(Direction side, int value) {
        var ord = side.ordinal();
        var old = _input[ord];
        _input[ord] = value;
        if (old >= 0 && old != value) {
            onRedstoneInputChanged(new RedstoneChangedEventArgs(side, old, value, -1));
        }
    }

    @Override
    public void setInput(int[] values) {
        for (var side : Direction.values()) {
            int value = side.ordinal() < values.length ? values[side.ordinal()] : 0;
            setInput(side, value);
        }
    }

    @Override
    public int maxInput() {
        int max = 0;
        for (int v : _input) max = Math.max(max, Math.max(v, 0));
        return max;
    }

    @Override
    public int[] output() {
        return _output;
    }

    @Override
    public int[][] bundledInput() {
        return _bundledInput;
    }

    @Override
    public int[][] rednetInput() {
        return _rednetInput;
    }

    @Override
    public int[][] bundledOutput() {
        return _bundledOutput;
    }

    @Override
    public int getOutput(Direction side) {
        return _output[toLocal(side).ordinal()];
    }

    @Override
    public void setOutput(Direction side, int value) {
        var ord = toLocal(side).ordinal();
        if (_output[ord] != value) {
            _output[ord] = value;
            onRedstoneOutputChanged(side);
        }
    }

    @Override
    public void setOutput(Map<?, ?> values) {
        for (var side : Direction.values()) {
            var ord = toLocal(side).ordinal();
            var key = Integer.valueOf(ord);
            if (values.containsKey(key)) {
                var raw = values.get(key);
                if (raw instanceof Number num) {
                    setOutput(side, num.intValue());
                }
            }
        }
    }

    protected void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        if (node != null && node.network() != null && dummyNode != null) {
            node.connect(dummyNode);
            dummyNode.sendToNeighbors("redstone.changed", args);
        }
    }

}
