package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.tileentity.traits.Inventory;
import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;


import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

public class Printer extends li.cil.oc.core.impl.common.tileentity.traits.TileEntity implements li.cil.oc.api.network.Environment, Inventory, Rotatable, SidedEnvironment, li.cil.oc.api.util.StateAware, WorldlyContainer, DeviceInfo {
    public static BlockEntityType<Printer> TYPE;
    public static final int MAX_AMOUNT_MATERIAL = 256000;
    public static final int MAX_AMOUNT_INK = 100000;
    public static final int SLOT_MATERIAL = 0;
    public static final int SLOT_INK = 1;
    public static final int SLOT_OUTPUT = 2;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("printer3d")
            .withConnector(Settings.get().bufferConverter)
            .create();
    private final ItemStack[] items = new ItemStack[3];
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Printer,
            DeviceInfo.DeviceAttribute.Description, "3D Printer",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "Omni-Materializer T6.1"
    );
    public int amountMaterial = 0;
    public int amountInk = 0;
    public PrintData data = new PrintData();
    public boolean isActive = false;
    public int limit = 0;
    public ItemStack output = null;
    public double totalRequiredEnergy = 0.0;
    public double requiredEnergy = 0.0;

    public Printer(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public Node node() {
        return node;
    }

    public int x() {
        return worldPosition.getX();
    }

    public int y() {
        return worldPosition.getY();
    }

    public int z() {
        return worldPosition.getZ();
    }

    public Direction facing() {
        return Direction.NORTH;
    }

    public void facing(Direction value) {
    }

    public Direction toLocal(Direction global) {
        return li.cil.oc.core.impl.util.RotationHelper.toLocal(pitch(), facing(), global);
    }

    public Direction toGlobal(Direction local) {
        return li.cil.oc.core.impl.util.RotationHelper.toGlobal(pitch(), facing(), local);
    }

    public void onRotationChanged() {
    }

    public void onConnect(Node node) {
    }

    public void onDisconnect(Node node) {
    }

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
    public void dispose() {
        super.dispose();
        if (isServer()) node.remove();
    }

    public Object[] result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public boolean canConnect(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public Node sidedNode(Direction side) {
        return side != Direction.UP ? node : null;
    }

    @Override
    public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (output != null) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
        if (canPrint()) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    public boolean canPrint() {
        return !data.stateOff.isEmpty() && data.stateOff.size() <= Settings.get().maxPrintComplexity && data.stateOn.size() <= Settings.get().maxPrintComplexity;
    }

    public boolean isPrinting() {
        return output != null;
    }

    public double progress() {
        return (1 - requiredEnergy / totalRequiredEnergy) * 100;
    }

    public int timeRemaining() {
        return (int) (requiredEnergy / Settings.get().printerTickAmount / 20);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function() -- Resets the configuration.")
    public Object[] reset(Context context, Arguments args) {
        data = new PrintData();
        isActive = false;
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(value:string) -- Set a label.")
    public Object[] setLabel(Context context, Arguments args) {
        var label = args.optString(0, null);
        data.label = label != null ? label.substring(0, Math.min(24, label.length())) : null;
        isActive = false;
        return null;
    }

    @Callback(doc = "function():string -- Get the current label.")
    public Object[] getLabel(Context context, Arguments args) {
        return result(data.label);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(value:string) -- Set a tooltip.")
    public Object[] setTooltip(Context context, Arguments args) {
        var tooltip = args.optString(0, null);
        data.tooltip = tooltip != null ? tooltip.substring(0, Math.min(128, tooltip.length())) : null;
        isActive = false;
        return null;
    }

    @Callback(doc = "function():string -- Get the current tooltip.")
    public Object[] getTooltip(Context context, Arguments args) {
        return result(data.tooltip);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(value:number) -- Set light level.")
    public Object[] setLightLevel(Context context, Arguments args) {
        data.lightLevel = Math.clamp(args.checkInteger(0), 0, Settings.get().maxPrintLightLevel);
        isActive = false;
        return null;
    }

    @Callback(doc = "function():number -- Get light level.")
    public Object[] getLightLevel(Context context, Arguments args) {
        return result(data.lightLevel);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(value:boolean or number) -- Set redstone emission.")
    public Object[] setRedstoneEmitter(Context context, Arguments args) {
        if (args.isBoolean(0)) data.redstoneLevel = args.checkBoolean(0) ? 15 : 0;
        else data.redstoneLevel = Math.clamp(args.checkInteger(0), 0, 15);
        isActive = false;
        return null;
    }

    @Callback(doc = "function():boolean, number -- Get redstone emission.")
    public Object[] isRedstoneEmitter(Context context, Arguments args) {
        return result(data.emitRedstone(), data.redstoneLevel);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(value:boolean) -- Set button mode.")
    public Object[] setButtonMode(Context context, Arguments args) {
        data.isButtonMode = args.checkBoolean(0);
        isActive = false;
        return null;
    }

    @Callback(doc = "function():boolean -- Get button mode.")
    public Object[] isButtonMode(Context context, Arguments args) {
        return result(data.isButtonMode);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(doc = "function(collideOff:boolean, collideOn:boolean) -- Set collidable.")
    public Object[] setCollidable(Context context, Arguments args) {
        data.noclipOff = !args.checkBoolean(0);
        data.noclipOn = !args.checkBoolean(1);
        return null;
    }

    @Callback(doc = "function():boolean, boolean -- Get collidable.")
    public Object[] isCollidable(Context context, Arguments args) {
        return result(!data.noclipOff, !data.noclipOn);
    }

    @Callback(doc = "function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean][,tint:number]) -- Adds a shape.")
    public Object[] addShape(Context context, Arguments args) {
        if (data.stateOff.size() > Settings.get().maxPrintComplexity || data.stateOn.size() > Settings.get().maxPrintComplexity) {
            return result(null, "model too complex");
        }
        float minX = Math.clamp(args.checkInteger(0), 0, 16) / 16f;
        float minY = Math.clamp(args.checkInteger(1), 0, 16) / 16f;
        float minZ = (16 - Math.clamp(args.checkInteger(2), 0, 16)) / 16f;
        float maxX = Math.clamp(args.checkInteger(3), 0, 16) / 16f;
        float maxY = Math.clamp(args.checkInteger(4), 0, 16) / 16f;
        float maxZ = (16 - Math.clamp(args.checkInteger(5), 0, 16)) / 16f;
        var texture = args.checkString(6).substring(0, Math.min(64, args.checkString(6).length()));
        boolean state = args.isBoolean(7) && args.checkBoolean(7);
        Integer tint = null;
        if (args.isInteger(7)) {
            tint = args.checkInteger(7);
        } else if (args.isInteger(8)) {
            tint = args.checkInteger(8);
        }
        if (minX == maxX || minY == maxY || minZ == maxZ) throw new IllegalArgumentException("empty block");
        var shape = new PrintData.Shape(new net.minecraft.world.phys.AABB(
                Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
                Math.max(maxX, minX), Math.max(maxY, minY), Math.max(maxZ, minZ)),
                texture, tint);
        if (state) data.stateOn.add(shape);
        else data.stateOff.add(shape);
        isActive = false;
        if (getLevel() != null) {
            getLevel().markAndNotifyBlock(worldPosition, getLevel().getChunkAt(worldPosition), getBlockState(), getBlockState(), 3, 512);
        }
        return result(true);
    }

    @Callback(doc = "function():number -- Get shape count.")
    public Object[] getShapeCount(Context context, Arguments args) {
        return result(data.stateOff.size(), data.stateOn.size());
    }

    @Callback(doc = "function():number -- Get max shape count.")
    public Object[] getMaxShapeCount(Context context, Arguments args) {
        return result(Settings.get().maxPrintComplexity);
    }

    @Callback(doc = "function([count:number]):boolean -- Commit and begin printing.")
    public Object[] commit(Context context, Arguments args) {
        if (!canPrint()) return result(null, "model invalid");
        limit = (int) Math.clamp(args.optDouble(0, 1), 0, Settings.get().maxPrintComplexity);
        isActive = limit > 0;
        return result(true);
    }

    @Callback(doc = "function(): string, number or boolean -- The current state.")
    public Object[] status(Context context, Arguments args) {
        if (output != null) return result("busy", progress());
        else if (canPrint()) return result("idle", true);
        else return result("idle", false);
    }

    public void updateEntity() {
        super.updateEntity();
        Supplier<Boolean> canMergeOutput = () -> {
            var presentStack = getItem(SLOT_OUTPUT);
            var outputStack = data.createItemStack();
            return presentStack.isEmpty() || (ItemStack.isSameItemSameComponents(presentStack, outputStack));
        };

        if (isActive && output == null && canMergeOutput.get()) {
            var costs = PrintData.computeCosts(data);
            if (costs != null) {
                totalRequiredEnergy = Settings.get().printCost;
                requiredEnergy = totalRequiredEnergy;
                if (amountMaterial >= costs[0] && amountInk >= costs[1]) {
                    amountMaterial -= costs[0];
                    amountInk -= costs[1];
                    limit -= 1;
                    output = data.createItemStack();
                    if (limit < 1) isActive = false;
                    PacketSender.sendPrinting(this, true);
                }
            } else {
                isActive = false;
                data = new PrintData();
            }
        }

        if (output != null) {
            double want = Math.clamp(requiredEnergy, 1, Settings.get().printerTickAmount);
            double have = want + (Settings.get().ignorePower ? 0 : ((li.cil.oc.api.network.Connector) node).changeBuffer(-want));
            requiredEnergy -= have;
            if (requiredEnergy <= 0) {
                var result = getItem(SLOT_OUTPUT);
                if (result.isEmpty()) {
                    setItem(SLOT_OUTPUT, output);
                } else if (result.getCount() < result.getMaxStackSize() && canMergeOutput.get()) {
                    result.grow(1);
                    setChanged();
                }
                requiredEnergy = 0;
                output = null;
            }
            PacketSender.sendPrinting(this, have > 0.5 && output != null);
        }

        int inputValue = PrintData.materialValue(getItem(SLOT_MATERIAL));
        if (inputValue > 0 && MAX_AMOUNT_MATERIAL - amountMaterial >= inputValue) {
            var material = removeItem(SLOT_MATERIAL, 1);
            if (!material.isEmpty()) amountMaterial += inputValue;
        }

        int inkValue = PrintData.inkValue(getItem(SLOT_INK));
        if (inkValue > 0 && MAX_AMOUNT_INK - amountInk >= inkValue) {
            var ink = removeItem(SLOT_INK, 1);
            if (!ink.isEmpty()) {
                amountInk += inkValue;
                if (ink.hasCraftingRemainingItem()) {
                    setItem(SLOT_INK, ink.getCraftingRemainingItem());
                }
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        if (nbt.contains(Settings.namespace + "node")) {
            node.load(nbt.getCompound(Settings.namespace + "node"), getEffectiveProvider());
        }
        amountMaterial = nbt.getInt(Settings.namespace + "amountMaterial");
        amountInk = nbt.getInt(Settings.namespace + "amountInk");
        var provider = getEffectiveProvider();
        if (provider != null) data.load(nbt.getCompound(Settings.namespace + "data"), provider);
        isActive = nbt.getBoolean(Settings.namespace + "active");
        limit = nbt.getInt(Settings.namespace + "limit");
        if (provider != null && nbt.contains(Settings.namespace + "output")) {
            output = ItemStack.parseOptional(provider, nbt.getCompound(Settings.namespace + "output"));
        }
        totalRequiredEnergy = nbt.getDouble(Settings.namespace + "total");
        requiredEnergy = nbt.getDouble(Settings.namespace + "remaining");
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        var tag = new CompoundTag();
        node.save(tag, getEffectiveProvider());
        nbt.put(Settings.namespace + "node", tag);
        nbt.putInt(Settings.namespace + "amountMaterial", amountMaterial);
        nbt.putInt(Settings.namespace + "amountInk", amountInk);
        li.cil.oc.core.impl.util.ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "data", t -> data.save(t, getEffectiveProvider()));
        nbt.putBoolean(Settings.namespace + "active", isActive);
        nbt.putInt(Settings.namespace + "limit", limit);
        if (output != null)
            li.cil.oc.core.impl.util.ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "output", t -> output.save(getEffectiveProvider(), t));
        nbt.putDouble(Settings.namespace + "total", totalRequiredEnergy);
        nbt.putDouble(Settings.namespace + "remaining", requiredEnergy);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (getLevel() != null) data.load(nbt.getCompound(Settings.namespace + "data"), getLevel().registryAccess());
        requiredEnergy = nbt.getDouble("remaining");
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        if (getLevel() != null)
            li.cil.oc.core.impl.util.ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "data", t -> data.save(t, getLevel().registryAccess()));
        nbt.putDouble("remaining", requiredEnergy);
    }

    @Override
    public ItemStack[] items() {
        return items;
    }

    @Override
    public void updateItems(int slot, ItemStack stack) {
        items[slot] = stack;
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty() && getItem(1).isEmpty() && getItem(2).isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        var stack = items[slot];
        return stack != null ? stack : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        var stack = getItem(slot);
        if (!stack.isEmpty()) {
            if (stack.getCount() <= amount) {
                setItem(slot, ItemStack.EMPTY);
            } else {
                stack = stack.split(amount);
                if (stack.getCount() == 0) setItem(slot, ItemStack.EMPTY);
            }
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        var stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        items[slot] = stack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        Arrays.fill(items, null);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == SLOT_MATERIAL) return PrintData.materialValue(stack) > 0;
        if (slot == SLOT_INK) return PrintData.inkValue(stack) > 0;
        return false;
    }

    @Override
    public boolean canTakeItem(@NotNull Container container, int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[]{SLOT_MATERIAL, SLOT_INK, SLOT_OUTPUT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, Direction side) {
        return slot != SLOT_OUTPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
        return !canPlaceItem(slot, stack);
    }

    public void spawnStackInWorld(ItemStack stack) {
    }

    public void spawnStackInWorld(ItemStack stack, Direction direction) {
    }

    public boolean isUseableByPlayer(Player player) {
        return true;
    }

    public void dropAllSlots() {
    }

    public void dropSlot(int slot) {
    }

    public void dropSlot(int slot, int count, Direction direction) {
    }

    @Override
    public void startOpen(@NotNull Player player) {
    }

    @Override
    public void stopOpen(@NotNull Player player) {
    }
}
