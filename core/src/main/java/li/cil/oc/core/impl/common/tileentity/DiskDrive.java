package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.Sound;
import li.cil.oc.core.impl.common.tileentity.traits.ComponentInventory;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.RotationHelper;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class DiskDrive extends TileEntity implements Environment, EnvironmentHost, ComponentInventory, li.cil.oc.core.impl.common.tileentity.traits.Rotatable, Analyzable, DeviceInfo {
    public static BlockEntityType<?> TYPE;

    public long lastAccess = 0;

    public final Component node = Network.newNode(this, Visibility.Network)
            .withComponent("disk_drive")
            .create();

    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Disk,
            DeviceInfo.DeviceAttribute.Description, "Floppy disk drive",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Spinner 520p1"
    );

    private final ItemStack[] _items = new ItemStack[getContainerSize()];
    private ManagedEnvironment[] _components;

    public DiskDrive(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        Arrays.fill(_items, ItemStack.EMPTY);
    }

    public Node filesystemNode() {
        var comps = _components();
        if (comps != null && comps.length > 0 && comps[0] != null) {
            return comps[0].node();
        }
        return null;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
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
        if (slot >= 0 && slot < _items.length) {
            _items[slot] = stack;
        }
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
        setChanged();
    }

    @Override
    public boolean isConnected() {
        return node.address() != null && node.network() != null;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void onRotationChanged() {
    }

    @Override
    public Direction facing() {
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public void facing(Direction value) {
        var state = getBlockState();
        if (getLevel() != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(BlockStateProperties.HORIZONTAL_FACING, value));
        }
    }

    @Override
    public Direction toLocal(Direction global) {
        return RotationHelper.toLocal(Direction.NORTH, facing(), global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return RotationHelper.toGlobal(Direction.NORTH, facing(), local);
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            connectComponents();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) {
            node.remove();
        }
    }

    @Override
    public Object result(Object... args) {
        return ResultWrapper.result(args);
    }

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(Context context, Arguments args) {
        return ResultWrapper.result(filesystemNode() == null);
    }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(Context context, Arguments args) {
        double velocity = Math.clamp(args.optDouble(0, 0), 0, 1);
        ItemStack stack = getItem(0);
        if (!stack.isEmpty()) {
            var ejected = stack.split(1);
            if (!ejected.isEmpty()) {
                var entity = InventoryUtils.spawnStackInWorld(BlockPosition.apply(this), ejected, facing(), null);
                if (entity != null) {
                    var dir = facing();
                    entity.addDeltaMovement(new net.minecraft.world.phys.Vec3(
                            dir.getStepX() * velocity,
                            dir.getStepY() * velocity,
                            dir.getStepZ() * velocity
                    ));
                }
                return ResultWrapper.result(true);
            }
        }
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(): string -- Return the internal floppy disk address")
    public Object[] media(Context context, Arguments args) {
        var fsNode = filesystemNode();
        if (fsNode == null) {
            return ResultWrapper.result(null, "drive is empty");
        }
        return ResultWrapper.result(fsNode.address());
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        var fsNode = filesystemNode();
        return fsNode != null ? new Node[]{fsNode} : null;
    }

    @Override
    public String inventoryName() {
        return "diskdrive";
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            var driver = Driver.driverFor(stack, getClass());
            return driver != null && Slot.Floppy.equals(driver.slot(stack));
        }
        return false;
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        li.cil.oc.core.impl.common.tileentity.traits.ComponentInventory.super.onItemAdded(slot, stack);
        var comps = _components();
        if (comps != null && slot >= 0 && slot < comps.length && comps[slot] != null) {
            var c = comps[slot].node();
            if (c instanceof Component component) {
                component.setVisibility(Visibility.Network);
            }
        }
        if (isServer()) {
            PacketSender.sendFloppyChange(this, stack);
            Sound.playDiskInsert(this);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        li.cil.oc.core.impl.common.tileentity.traits.ComponentInventory.super.onItemRemoved(slot, stack);
        if (isServer()) {
            PacketSender.sendFloppyChange(this, ItemStack.EMPTY);
            Sound.playDiskEject(this);
        }
    }

    private static final String DiskTag = Settings.namespace + "disk";

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (nbt.contains(DiskTag)) {
            if (getLevel() != null) {
                setItem(0, ItemStack.parseOptional(getLevel().registryAccess(), nbt.getCompound(DiskTag)));
            }
        }
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        if (!getItem(0).isEmpty()) {
            var tag = new CompoundTag();
            if (getLevel() != null) {
                getItem(0).save(getLevel().registryAccess(), tag);
                nbt.put(DiskTag, tag);
            }
        }
    }

    @Override
    public EnvironmentHost host() {
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
    public ArrayList<ManagedEnvironment> updatingComponents() {
        return new ArrayList<>();
    }

    @Override
    public void spawnStackInWorld(ItemStack stack) {
        if (isServer()) {
            InventoryUtils.spawnStackInWorld(position(), stack, null, null);
        }
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction side) {
        if (isServer()) {
            InventoryUtils.spawnStackInWorld(position(), stack, side, null);
        }
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
            var toDrop = stack.copy();
            toDrop.setCount(Math.min(count, stack.getCount()));
            if (direction != null) spawnStackInWorld(toDrop, direction);
            else spawnStackInWorld(toDrop);
            setItem(slot, ItemStack.EMPTY);
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
}
