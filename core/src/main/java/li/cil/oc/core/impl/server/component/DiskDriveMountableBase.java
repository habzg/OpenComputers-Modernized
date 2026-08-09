package li.cil.oc.core.impl.server.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.common.Sound;
import li.cil.oc.core.impl.common.inventory.ComponentInventory;
import li.cil.oc.core.impl.common.inventory.ItemStackInventory;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class DiskDriveMountableBase extends AbstractManagedEnvironment implements ItemStackInventory, ComponentInventory, RackMountable, Analyzable, DeviceInfo, ItemStackInventory.ItemStackInventoryAccessor {
    public final Rack rack;
    public final int slot;
    public long lastAccess = 0L;

    private final ItemStack[] _items = new ItemStack[getContainerSize()];
    private ManagedEnvironment[] _components = new ManagedEnvironment[getContainerSize()];
    private final ArrayList<ManagedEnvironment> _updatingComponents = new ArrayList<>();

    @SuppressWarnings("unused")
    public final Component node = Network.newNode(this, Visibility.Network)
            .withComponent("disk_drive")
            .create();

    private final Map<String, String> deviceInfo = Map.of(
            DeviceAttribute.Class, DeviceClass.Disk,
            DeviceAttribute.Description, "Floppy disk drive",
            DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceAttribute.Product, "RackDrive 100 Rev. 2"
    );

    public DiskDriveMountableBase(Rack rack, int slot) {
        super(rack);
        this.rack = rack;
        this.slot = slot;
        Arrays.fill(_items, ItemStack.EMPTY);
    }

    public Node filesystemNode() {
        if (_components[0] != null) return _components[0].node();
        return null;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(Context context, Arguments args) {
        Node fs = filesystemNode();
        return ResultWrapper.result(fs == null);
    }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(Context context, Arguments args) {
        double velocity = Math.clamp(args.optDouble(0, 0), 0, 1);
        ItemStack ejected = removeItem(0, 1);
        if (!ejected.isEmpty()) {
            var pos = BlockPosition.apply(rack);
            var entity = InventoryUtils.spawnStackInWorld(pos, ejected, rack.facing(), null);
            if (entity != null) {
                double vx = rack.facing().getStepX() * velocity;
                double vy = rack.facing().getStepY() * velocity;
                double vz = rack.facing().getStepZ() * velocity;
                entity.push(vx, vy, vz);
            }
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(): string -- Return the internal floppy disk address")
    public Object[] media(Context context, Arguments args) {
        Node fs = filesystemNode();
        if (fs == null) return ResultWrapper.result(null, "drive is empty");
        return ResultWrapper.result(fs.address());
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        Node fs = filesystemNode();
        return new Node[]{fs};
    }

    @Override
    public ItemStack container() {
        return rack.getItem(slot);
    }

    @Override
    public ItemStack[] getItemsArray() {
        return _items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        if (slot >= 0 && slot < _items.length) {
            _items[slot] = stack;
        }
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
        return _updatingComponents;
    }

    @Override
    public EnvironmentHost host() {
        return rack;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            var driver = Driver.driverFor(stack);
            return driver != null && Slot.Floppy.equals(driver.slot(stack));
        }
        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return rack.stillValid(player);
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        ComponentInventory.super.onItemAdded(slot, stack);
        if (_components[slot] != null && _components[slot].node() instanceof Component c) {
            c.setVisibility(Visibility.Network);
        }
        if (!rack.level().isClientSide) {
            rack.markChanged(this.slot);
            Sound.playDiskInsert(rack);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        ComponentInventory.super.onItemRemoved(slot, stack);
        if (!rack.level().isClientSide) {
            rack.markChanged(this.slot);
            Sound.playDiskEject(rack);
        }
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        ComponentInventory.super.load(nbt, provider);
        connectComponents();
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        ComponentInventory.super.save(nbt, provider);
    }

    @Override
    public CompoundTag getData() {
        var nbt = new CompoundTag();
        nbt.putLong("lastAccess", lastAccess);
        var provider = rack.level().registryAccess();
        var stack = getItem(0);
        if (!stack.isEmpty()) {
            nbt.put("disk", stack.save(provider, new CompoundTag()));
        }
        return nbt;
    }

    @Override
    public int getConnectableCount() {
        return 0;
    }

    @Override
    public RackBusConnectable getConnectableAt(int index) {
        return null;
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (player.isShiftKeyDown()) {
            boolean isDiskInDrive = !getItem(0).isEmpty();
            boolean isHoldingDisk = canPlaceItem(0, heldItem);
            if (isDiskInDrive) {
                if (!rack.level().isClientSide) {
                    InventoryUtils.dropSlot(BlockPosition.apply(rack), this, 0, 1, rack.facing());
                }
            }
            if (isHoldingDisk) {
                setItem(0, heldItem.split(1));
            }
            return isDiskInDrive || isHoldingDisk;
        } else {
            openDiskDriveGui(player, BlockPosition.apply(rack), slot);
            return true;
        }
    }

    protected abstract void openDiskDriveGui(Player player, BlockPosition ignoredPos, int slot);

    @Override
    public EnumSet<State> getCurrentState() {
        return EnumSet.noneOf(State.class);
    }
}
