package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.SidedBlock;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.tileentity.traits.ComponentInventory;
import li.cil.oc.core.impl.common.tileentity.traits.OpenSides;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class Adapter extends TileEntity implements li.cil.oc.api.network.Environment, ComponentInventory, OpenSides, Analyzable, li.cil.oc.api.internal.Adapter, DeviceInfo {
    public static BlockEntityType<?> TYPE;

    public final Node node = Network.newNode(this, Visibility.Network).create();
    private final li.cil.oc.api.network.Environment[] blocks = new li.cil.oc.api.network.Environment[6];
    private final ArrayList<ManagedEnvironment> updatingBlocks = new ArrayList<>();
    private final BlockData[] blocksData = new BlockData[6];
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Bus,
            DeviceInfo.DeviceAttribute.Description, "Adapter",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Multiplug Ext.1"
    );
    private final ItemStack[] _items = new ItemStack[getContainerSize()];
    private final boolean[] _openSides = {true, true, true, true, true, true};
    private ManagedEnvironment[] _components;

    public Adapter(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        for (int i = 0; i < 6; i++) {
            blocks[i] = null;
            blocksData[i] = null;
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
    public ItemStack[] items() {
        return _items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        _items[slot] = stack;
    }

    @Override
    public int x() {
        return worldPosition.getX();
    }

    @Override
    public int y() {
        return worldPosition.getY();
    }

    @Override
    public int z() {
        return worldPosition.getZ();
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
    public void initialize() {
        super.initialize();
        if (level() != null && isServer()) Network.joinOrCreateNetwork(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer() && node != null) {
            node.remove();
        }
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            connectComponents();
            neighborChanged();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        ComponentInventory.super.onDisconnect(node);
        if (node == this.node) updatingBlocks.clear();
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
    }

    @Override
    public Object result(Object... args) {
        return ResultWrapper.result(args);
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean[] openSides() {
        return _openSides;
    }

    @Override
    public void openSides(boolean[] value) {
        if (value.length == _openSides.length) {
            System.arraycopy(value, 0, _openSides, 0, _openSides.length);
        }
    }

    @Override
    public boolean isSideOpen(Direction side) {
        return _openSides[side.ordinal()];
    }

    @Override
    public void setSideOpen(Direction side, boolean value) {
        _openSides[side.ordinal()] = value;
        if (isServer()) {
            PacketSender.sendAdapterState(this, compressSides());
            level().playSeededSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.PISTON_EXTEND, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.5f, level().random.nextFloat() * 0.25f + 0.7f, level().random.nextLong());
            level().updateNeighborsAt(worldPosition, block());
            neighborChanged(side);
        } else {
            level().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public byte compressSides() {
        byte result = 0;
        for (int i = 0; i < 6; i++) {
            if (_openSides[i]) result |= (byte) (1 << i);
        }
        return result;
    }

    @Override
    public void uncompressSides(byte value) {
        for (int i = 0; i < 6; i++) {
            _openSides[i] = (value & (1 << i)) != 0;
        }
    }

    @Override
    public li.cil.oc.api.network.EnvironmentHost host() {
        return this;
    }

    @Override
    public ManagedEnvironment[] _components() {
        return _components;
    }

    @Override
    public void _components(ManagedEnvironment[] value) {
        _components = value;
    }

    @Override
    public java.util.ArrayList<ManagedEnvironment> updatingComponents() {
        return updatingBlocks;
    }

    @Override
    public void spawnStackInWorld(ItemStack stack) {
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction side) {
    }

    @Override
    public void dropAllSlots() {
        for (int i = 0; i < getContainerSize(); i++) {
            dropSlot(i);
        }
    }

    @Override
    public void dropSlot(int slot) {
        dropSlot(slot, 1, null);
    }

    @Override
    public void dropSlot(int slot, int count, Direction direction) {
        var stack = getItem(slot);
        if (!stack.isEmpty()) {
            var toDrop = stack.split(count);
            if (direction != null) spawnStackInWorld(toDrop, direction);
            else spawnStackInWorld(toDrop);
        }
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        var result = new ArrayList<Node>();
        for (var block : blocks)
            if (block != null) {
                var n = block.node();
                if (n != null) result.add(n);
            }
        ManagedEnvironment[] adapterComps = this.componentEnvironments();
        for (var comp : adapterComps)
            if (comp != null) {
                var n = comp.node();
                if (n != null) result.add(n);
            }
        return result.toArray(new Node[0]);
    }

    public void updateEntity() {
        super.updateEntity();
        if (!updatingBlocks.isEmpty()) {
            for (var block : updatingBlocks) block.update();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void neighborChanged(Direction d) {
        if (node.network() != null) {
            var nx = worldPosition.getX() + d.getStepX();
            var ny = worldPosition.getY() + d.getStepY();
            var nz = worldPosition.getZ() + d.getStepZ();
            var te = level().getBlockEntity(new BlockPos(nx, ny, nz));
            if (te instanceof Environment) return;
            var newDriver = Driver.driverFor(level(), new BlockPos(nx, ny, nz), d);
            if (newDriver != null && isSideOpen(d)) {
                if (blocks[d.ordinal()] != null) {
                    var oldEnv = blocks[d.ordinal()];
                    var driver = blocksData[d.ordinal()] != null ? blocksData[d.ordinal()].driver : null;
                    if (!Objects.equals(newDriver, driver)) {
                        blocks[d.ordinal()] = null;
                        updatingBlocks.remove(oldEnv);
                        blocksData[d.ordinal()] = null;
                        node.disconnect(oldEnv.node());
                        var environment = newDriver.createEnvironment(level(), nx, ny, nz, d);
                        if (environment != null) {
                            blocks[d.ordinal()] = environment;
                            if (environment.canUpdate()) updatingBlocks.add(environment);
                            blocksData[d.ordinal()] = new BlockData(environment.getClass().getName(), new CompoundTag(), newDriver);
                            node.connect(environment.node());
                        }
                    }
                } else {
                    if (!isSideOpen(d)) return;
                    var environment = newDriver.createEnvironment(level(), nx, ny, nz, d);
                    if (environment != null) {
                        blocks[d.ordinal()] = environment;
                        if (environment.canUpdate()) updatingBlocks.add(environment);
                        if (blocksData[d.ordinal()] != null && blocksData[d.ordinal()].name.equals(environment.getClass().getName())) {
                            environment.load(blocksData[d.ordinal()].data, level().registryAccess());
                        }
                        blocksData[d.ordinal()] = new BlockData(environment.getClass().getName(), new CompoundTag(), newDriver);
                        node.connect(environment.node());
                    }
                }
            } else {
                if (blocks[d.ordinal()] != null) {
                    var environment = (ManagedEnvironment) blocks[d.ordinal()];
                    node.disconnect(environment.node());
                    environment.save(blocksData[d.ordinal()].data, level().registryAccess());
                    if (environment.node() != null) environment.node().remove();
                    blocks[d.ordinal()] = null;
                    updatingBlocks.remove(environment);
                }
            }
        }
    }

    public void neighborChanged() {
        if (node.network() != null) {
            for (var d : Direction.values()) neighborChanged(d);
        }
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            var driver = Driver.driverFor(stack, getClass());
            return driver != null && driver.slot(stack).equals(Slot.Upgrade);
        }
        return false;
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        if (nbt.contains(Settings.namespace + "openSides")) {
            uncompressSides(nbt.getByte(Settings.namespace + "openSides"));
        }
        var blocksNbt = nbt.getList(Settings.namespace + "adapter.blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(blocksNbt.size(), blocksData.length); i++) {
            var blockNbt = blocksNbt.getCompound(i);
            if (blockNbt.contains("name") && blockNbt.contains("data")) {
                blocksData[i] = new BlockData(blockNbt.getString("name"), blockNbt.getCompound("data"));
            }
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        nbt.putByte(Settings.namespace + "openSides", compressSides());
        var blocksNbt = new ListTag();
        for (int i = 0; i < blocks.length; i++) {
            var blockNbt = new CompoundTag();
            if (blocksData[i] != null) {
                if (blocks[i] != null)
                    ((ManagedEnvironment) blocks[i]).save(blocksData[i].data, getEffectiveProvider());
                blockNbt.putString("name", blocksData[i].name);
                blockNbt.put("data", blocksData[i].data);
            }
            blocksNbt.add(blockNbt);
        }
        nbt.put(Settings.namespace + "adapter.blocks", blocksNbt);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (nbt.contains(Settings.namespace + "openSides")) {
            uncompressSides(nbt.getByte(Settings.namespace + "openSides"));
        }
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putByte(Settings.namespace + "openSides", compressSides());
    }

    @Override
    public ItemStack[] pendingRemovals() {
        return null;
    }

    @Override
    public ItemStack[] pendingAdds() {
        return null;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
    }

    private record BlockData(String name, CompoundTag data, SidedBlock driver) {
        BlockData(String name, CompoundTag data) {
            this(name, data, null);
        }
    }

}
