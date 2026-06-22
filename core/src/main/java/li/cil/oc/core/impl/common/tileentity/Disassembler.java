package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.template.DisassemblerTemplates;
import li.cil.oc.core.impl.common.tileentity.traits.Inventory;
import li.cil.oc.core.impl.common.tileentity.traits.PlayerInputAware;
import li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

public class Disassembler extends TileEntity implements li.cil.oc.api.network.Environment, PowerAcceptor, Inventory, li.cil.oc.api.util.StateAware, PlayerInputAware, DeviceInfo {

    public static BlockEntityType<Disassembler> TYPE;
    public final Node node = Network.newNode(this, Visibility.None)
            .withConnector(Settings.get().bufferConverter)
            .create();
    public final ArrayList<ItemStack> queue = new ArrayList<>();
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Disassembler",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Break.3R-100"
    );
    public boolean isActive = false;
    public double totalRequiredEnergy = 0;
    public double buffer = 0;
    public boolean disassembleNextInstantly = false;
    private final ItemStack[] _items = new ItemStack[getContainerSize()];

    public Disassembler(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public Node node() {
        return node;
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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public double globalBuffer(Direction side) {
        if (node instanceof li.cil.oc.api.network.Connector c) {
            return c.globalBuffer();
        }
        return 0;
    }

    @Override
    public double globalBufferSize(Direction side) {
        if (node instanceof li.cil.oc.api.network.Connector c) {
            return c.globalBufferSize();
        }
        return 0;
    }

    @Override
    public double globalDemand(Direction side) {
        if (node instanceof li.cil.oc.api.network.Connector c) {
            return Math.clamp(c.globalBufferSize() - c.globalBuffer(), 0, energyThroughput());
        }
        return 0;
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount) {
        return tryChangeBuffer(side, amount, true);
    }

    @Override
    public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
        if (node instanceof li.cil.oc.api.network.Connector c) {
            if (c.tryChangeBuffer(amount)) return amount;
            else return 0;
        }
        return 0;
    }

    @Override
    public boolean canConnectPower(Direction side) {
        return hasConnector(side);
    }

    public double progress() {
        return queue.isEmpty() ? 0.0 : (1 - (queue.size() * Settings.get().disassemblerItemCost - buffer) / totalRequiredEnergy) * 100;
    }

    private void setActive(boolean value) {
        if (value != isActive) {
            isActive = value;
            PacketSender.sendDisassemblerActive(this, isActive);
            var level = getLevel();
            if (level != null) level.updateNeighborsAt(worldPosition, block());
            setChanged();
        }
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    protected boolean hasConnector(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public double energyThroughput() {
        return Settings.get().disassemblerRate;
    }

    @Override
    public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (isActive) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        else if (!queue.isEmpty()) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        else return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
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
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) node.remove();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        var level = getLevel();
        if (level != null && level.getGameTime() % Settings.get().tickFrequency == 0) {
            if (queue.isEmpty()) {
                boolean instant = disassembleNextInstantly;
                disassemble(removeItem(0, 1), instant);
                setActive(!queue.isEmpty());
            } else {
                if (buffer < Settings.get().disassemblerItemCost) {
                    double want = Settings.get().disassemblerTickAmount;
                    boolean success = ((li.cil.oc.api.network.Connector) node).tryChangeBuffer(-want);
                    setActive(success);
                    if (success) buffer += want;
                }
                while (buffer >= Settings.get().disassemblerItemCost && !queue.isEmpty()) {
                    buffer -= Settings.get().disassemblerItemCost;
                    var stack = queue.removeFirst();
                    if (disassembleNextInstantly || level.random.nextDouble() >= Settings.get().disassemblerBreakChance) {
                        drop(stack);
                    }
                }
            }
            disassembleNextInstantly = !queue.isEmpty();
        }
    }

    public void disassemble(ItemStack stack, boolean instant) {
        if (canPlaceItem(0, stack)) {
            var ingredients = ItemUtils.getIngredients(stack);
            var template = DisassemblerTemplates.select(stack);
            if (template != null) {
                var result = template.disassemble(stack, ingredients);
                if (result[0] != null) queue.addAll(java.util.Arrays.asList((ItemStack[]) result[0]));
                if (result[1] != null) for (var drop : (ItemStack[]) result[1]) drop(drop);
            } else {
                queue.addAll(java.util.Arrays.asList(ingredients));
            }
            totalRequiredEnergy = queue.size() * Settings.get().disassemblerItemCost;
            if (instant) buffer = totalRequiredEnergy;
        } else {
            drop(stack);
        }
    }

    @Override
    public void spawnStackInWorld(ItemStack stack) {
        spawnStackInWorld(stack, null);
    }

    @Override
    public void spawnStackInWorld(ItemStack stack, Direction side) {
        li.cil.oc.core.impl.util.InventoryUtils.spawnStackInWorld(
                li.cil.oc.core.impl.util.BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()),
                stack, side, null);
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

    private void drop(ItemStack stack) {
        if (stack != null) {
            for (var side : Direction.values()) {
                if (stack.getCount() <= 0) break;
                InventoryUtils.insertIntoInventoryAt(stack, BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()).offset(side), side.getOpposite());
            }
            if (stack.getCount() > 0) spawnStackInWorld(stack, Direction.UP);
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        queue.clear();
        var tagList = nbt.getList(Settings.namespace + "queue", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            queue.add(ItemStack.parseOptional(getEffectiveProvider(), tagList.getCompound(i)));
        }
        buffer = nbt.getDouble(Settings.namespace + "buffer");
        totalRequiredEnergy = nbt.getDouble(Settings.namespace + "total");
        isActive = !queue.isEmpty();
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        var tagList = new ListTag();
        for (var stack : queue) tagList.add(stack.save(getEffectiveProvider(), new CompoundTag()));
        nbt.put(Settings.namespace + "queue", tagList);
        nbt.putDouble(Settings.namespace + "buffer", buffer);
        nbt.putDouble(Settings.namespace + "total", totalRequiredEnergy);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        isActive = nbt.getBoolean("isActive");
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putBoolean("isActive", isActive);
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
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int i, @NotNull ItemStack stack) {
        return allowDisassembling(stack) &&
                ((Settings.get().disassembleAllTheThings || li.cil.oc.api.Items.get(stack) != null) && ItemUtils.getIngredients(stack).length > 0 ||
                        DisassemblerTemplates.select(stack) != null);
    }

    private boolean allowDisassembling(ItemStack stack) {
        if (stack == null) return false;
        CustomData _cd = stack.get(DataComponents.CUSTOM_DATA);
        return _cd == null || _cd.isEmpty() || !_cd.copyTag().getBoolean(Settings.namespace + "undisassemblable");
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        Inventory.super.setItem(slot, stack);
        var level = getLevel();
        if (level != null && !level.isClientSide) disassembleNextInstantly = false;
    }

    @Override
    public void onSetInventorySlotContents(net.minecraft.world.entity.player.Player player, int slot, ItemStack stack) {
        var level = getLevel();
        if (level != null && !level.isClientSide) {
            disassembleNextInstantly = stack != null && slot == 0 && player.getAbilities().instabuild;
        }
    }
}
