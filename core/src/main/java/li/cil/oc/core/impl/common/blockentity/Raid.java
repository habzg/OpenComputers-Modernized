package li.cil.oc.core.impl.common.blockentity;

import java.util.UUID;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.item.data.DriveData;
import li.cil.oc.core.impl.common.blockentity.traits.Inventory;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.server.component.FileSystem;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Raid extends BlockEntity implements li.cil.oc.api.network.Environment, li.cil.oc.api.network.EnvironmentHost, Inventory, Rotatable, Analyzable {
    public static BlockEntityType<Raid> TYPE;
    private static final Logger LOGGER = LoggerFactory.getLogger(Raid.class);
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.None).create();
    public final RaidLabel label = new RaidLabel();
    public final boolean[] presence;
    public FileSystem filesystem = null;
    public long lastAccess = 0;

    public Raid(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        presence = new boolean[getContainerSize()];
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Direction facing() {
        var state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public void facing(Direction value) {
        var state = getBlockState();
        if (getLevel() != null && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            getLevel().setBlockAndUpdate(worldPosition, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, value));
        }
    }

    @Override
    public Direction toLocal(Direction global) {
        return li.cil.oc.core.impl.util.RotationHelper.toLocal(pitch(), facing(), global);
    }

    @Override
    public Direction toGlobal(Direction local) {
        return li.cil.oc.core.impl.util.RotationHelper.toGlobal(pitch(), facing(), local);
    }

    @Override
    public void onRotationChanged() {
        setChanged();
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
        setChanged();
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
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return filesystem != null ? new Node[]{filesystem.node()} : null;
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
        return driver != null && driver.slot(stack).equals(Slot.HDD);
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) synchronized (this) {
            boolean[] slots = new boolean[getContainerSize()];
            for (int i = 0; i < slots.length; i++) slots[i] = !getItem(i).isEmpty();
            PacketSender.sendRaidChange(this, slots);
            tryCreateRaid(UUID.randomUUID().toString());
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        for (int i = 0; i < presence.length; i++) presence[i] = items()[i] != null;
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        if (isServer()) synchronized (this) {
            boolean[] slots = new boolean[getContainerSize()];
            for (int i = 0; i < slots.length; i++) slots[i] = !getItem(i).isEmpty();
            PacketSender.sendRaidChange(this, slots);
            if (filesystem != null) {
                filesystem.fileSystem.close();
                for (var f : filesystem.fileSystem.list("/")) filesystem.fileSystem.delete(f);
                if (getLevel() != null) filesystem.save(new CompoundTag(), getLevel().registryAccess());
                filesystem.node().remove();
                filesystem = null;
            }
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
    public boolean isUseableByPlayer(Player player) {
        return true;
    }

    @Override
    public void spawnStackInWorld(ItemStack stack) {
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction direction) {
    }

    @Override
    public void dropAllSlots() {
        for (int i = 0; i < getContainerSize(); i++) {
            dropSlot(i);
        }
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void dropSlot(int slot) {
        dropSlot(slot, 1, null);
    }

    @Override
    public void dropSlot(int slot, int count, Direction direction) {
    }

    private final ItemStack[] _items = new ItemStack[getContainerSize()];

    @Override
    public ItemStack[] items() {
        return _items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        _items[slot] = stack;
    }

    public void tryCreateRaid(String id) {
        if (!isValidUUID(id)) {
            LOGGER.warn("Invalid RAID node address '{}', generating fresh UUID", id);
            id = UUID.randomUUID().toString();
        }
        var filled = true;
        for (var item : items())
            if (item == null) {
                filled = false;
                break;
            }
        if (filled && (filesystem == null || filesystem.node() == null || !id.equals(filesystem.node().address()))) {
            if (filesystem != null) {
                if (filesystem.node() != null) filesystem.node().remove();
            }
            if (getLevel() != null) {
                for (var fsStack : items()) {
                    if (fsStack != null) {
                        var drive = new DriveData(fsStack);
                        drive.lockInfo = "";
                        drive.isUnmanaged = false;
                        drive.save(fsStack, getLevel().registryAccess());
                    }
                }
            }
            long space = wipeDisksAndComputeSpace();
            li.cil.oc.api.fs.FileSystem rawFs = li.cil.oc.api.FileSystem.fromSaveDirectory(id, space, OCSettings.get().bufferChanges);
            if (rawFs == null) return;
            var fs = (FileSystem) li.cil.oc.api.FileSystem.asManagedEnvironment(
                    rawFs, label, this, OCSettings.resourceDomain + ":hdd_access", 6);
            var nbtToSetAddress = new CompoundTag();
            nbtToSetAddress.putString("address", id);
            if (getLevel() != null) {
                fs.node().load(nbtToSetAddress, getLevel().registryAccess());
            }
            li.cil.oc.api.Network.joinNewNetwork(node);
            node.connect(fs.node());
            ((li.cil.oc.api.network.Component) fs.node()).setVisibility(Visibility.Network);
            filesystem = fs;
        }
    }

    private long wipeDisksAndComputeSpace() {
        long acc = 0;
        for (var hdd : items()) {
            if (hdd != null) {
                var driver = li.cil.oc.api.API.driver.driverFor(hdd);
                if (driver != null) {
                    var env = driver.createEnvironment(hdd, this);
                    if (env instanceof FileSystem fs) {
                        var nbt = driver.dataTag(hdd);
                        if (getLevel() != null) {
                            fs.load(nbt, getLevel().registryAccess());
                        }
                        fs.fileSystem.close();
                        for (var f : fs.fileSystem.list("/")) fs.fileSystem.delete(f);
                        if (getLevel() != null) {
                            fs.save(nbt, getLevel().registryAccess());
                        }
                        acc += fs.fileSystem.spaceTotal();
                    }
                }
            }
        }
        return acc;
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        var loadProvider = getEffectiveProvider();
        if (loadProvider != null) load(nbt, loadProvider);
        if (nbt.contains(OCSettings.namespace + "fs")) {
            var tag = nbt.getCompound(OCSettings.namespace + "fs");
            tryCreateRaid(tag.getCompound("node").getString("address"));
            if (filesystem != null) {
                var provider = getEffectiveProvider();
                if (provider != null) {
                    filesystem.load(tag, provider);
                }
            }
        }
        if (getEffectiveProvider() != null) label.load(nbt, getEffectiveProvider());
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        var provider = getEffectiveProvider();
        if (provider != null) save(nbt, provider);
        if (filesystem != null)
            ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "fs", t -> filesystem.save(t, provider));
        label.save(nbt, provider);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        var bytes = nbt.getByteArray("presence");
        for (int i = 0; i < Math.min(bytes.length, presence.length); i++) presence[i] = bytes[i] != 0;
        label.setLabel(nbt.getString("label"));
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        var inv = items();
        var bytes = new byte[inv.length];
        for (int i = 0; i < inv.length; i++) bytes[i] = (byte) (inv[i] != null && !inv[i].isEmpty() ? 1 : 0);
        nbt.putByteArray("presence", bytes);
        if (label.getLabel() != null) nbt.putString("label", label.getLabel());
    }

    private static boolean isValidUUID(String s) {
        if (s == null) return false;
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static class RaidLabel implements Label {
        public String label = "raid";

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public void setLabel(String value) {
            label = value != null ? value.substring(0, Math.min(16, value.length())) : null;
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            if (nbt.contains(OCSettings.namespace + "label")) label = nbt.getString(OCSettings.namespace + "label");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.putString(OCSettings.namespace + "label", label);
        }
    }
}
