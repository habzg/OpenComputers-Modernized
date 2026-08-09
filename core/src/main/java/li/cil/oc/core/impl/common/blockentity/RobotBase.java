package li.cil.oc.core.impl.common.blockentity;

import com.google.common.collect.ArrayListMultimap;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.inventory.InventorySelection;
import li.cil.oc.core.common.inventory.TankSelection;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.inventory.InventoryProxy;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RobotBase extends BlockEntity implements li.cil.oc.core.impl.common.blockentity.traits.Environment, Agent, Container, InventorySelection, TankSelection, Environment, li.cil.oc.core.impl.common.blockentity.traits.BundledRedstoneAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobotBase.class);
    public static final int MAX_INVENTORY_SIZE = 100;
    public final RobotData info = new RobotData();
    public Object bot;
    public final Machine machine;
    public final InventoryProxy equipmentInventory;
    public final InventoryProxy mainInventory;
    public Class<? extends EnvironmentHost> hostClass;
    public final ItemStack[] inventory = new ItemStack[MAX_INVENTORY_SIZE];
    public int inventorySize = -1;
    public volatile int selectedSlot = 0;
    public volatile int selectedTank = 0;
    public double globalBuffer, globalBufferSize;
    public String ownerName;
    public UUID ownerUUID;
    public int animationTicksLeft = 0, animationTicksTotal = 0;
    public volatile int moveFromX = Integer.MAX_VALUE, moveFromY = Integer.MAX_VALUE, moveFromZ = Integer.MAX_VALUE;
    public volatile boolean swingingTool = false;
    public volatile int turnAxis = 0;
    public boolean appliedToolEnchantments = false;
    public boolean renderingErrored = false;
    public boolean pendingOnLoadSkip = false;

    public li.cil.oc.api.network.Node node;
    public final int[] input = new int[6];
    public final int[] output = new int[6];

    public final int[][] bundledInput = new int[6][16];
    public final int[][] bundledOutput = new int[6][16];

    public final Set<String> users = new HashSet<>();
    public volatile Direction facing = Direction.NORTH;
    public volatile boolean isRunning = false;
    public volatile boolean hasErrored = false;
    public boolean isOutputEnabled = false;
    public boolean shouldUpdateInput = false;

    public RobotBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ownerName = OCSettings.get().fakePlayerName;
        ownerUUID = OCSettings.get().fakePlayerProfile.getId();
        Arrays.fill(inventory, ItemStack.EMPTY);
        machine = li.cil.oc.api.Machine.create(this);
        for (int i = 0; i < 6; i++) {
            Arrays.fill(bundledInput[i], -1);
        }
        equipmentInventory = new InventoryProxy() {
            @Override
            public Container inventory() {
                return RobotBase.this;
            }

            @Override
            public int getContainerSize() {
                return 4;
            }

            @Override
            public void clearContent() {
                for (int i = 0; i < getContainerSize(); i++) {
                    setItem(i, ItemStack.EMPTY);
                }
            }
        };
        mainInventory = new InventoryProxy() {
            @Override
            public Container inventory() {
                return RobotBase.this;
            }

            @Override
            public int offset() {
                return equipmentInventory.getContainerSize();
            }

            @Override
            public int getContainerSize() {
                return RobotBase.this.inventorySize >= 0 ? RobotBase.this.inventorySize : 16;
            }

            @Override
            public void clearContent() {
                int start = equipmentInventory.getContainerSize();
                int end = start + getContainerSize();
                for (int i = start; i < end; i++) {
                    setItem(i, ItemStack.EMPTY);
                }
            }
        };
        if (machine != null) {
            node = machine.node();
            machine.setCostPerTick(OCSettings.get().robotCost);
        }
    }

    public abstract MultiTank tank();

    public abstract Player player();

    public abstract void rotateProxy(Direction axis);

    public abstract void sendToReachable(String message, Object data);

    public Node node() {
        return node;
    }

    public void ensureNodeAddress() {
        if (node() != null && node().address() == null) {
            li.cil.oc.api.Network.joinNewNetwork(node());
        }
    }

    public void saveComponents(HolderLookup.Provider provider) {
        if (this instanceof li.cil.oc.core.impl.common.inventory.ComponentInventory ci) {
            li.cil.oc.core.impl.common.inventory.ComponentInventory.saveComponents(ci, provider);
        }
    }

    @Override
    public boolean isServer() {
        var l = getLevel();
        return l != null && !l.isClientSide();
    }

    @Override
    public boolean isClient() {
        var l = getLevel();
        return l != null && l.isClientSide();
    }

    public boolean isCreative() {
        return info.tier == Tier.Four;
    }

    public int componentCount() {
        return info.components.size();
    }

    public int maxInventorySize() {
        return MAX_INVENTORY_SIZE - equipmentInventory.getContainerSize() - componentCount();
    }

    public Set<Integer> containerSlots() {
        var result = new HashSet<Integer>();
        for (int i = 0; i < info.containers.size(); i++) result.add(i + 1);
        return result;
    }

    public Set<Integer> componentSlots() {
        var result = new HashSet<Integer>();
        int totalSize = getContainerSize();
        int offset = Math.max(totalSize - componentCount(), equipmentInventory.getContainerSize());
        for (int i = 0; i < componentCount(); i++) result.add(offset + i);
        return result;
    }

    public Set<Integer> inventorySlots() {
        var result = new HashSet<Integer>();
        int start = equipmentInventory.getContainerSize();
        int end = start + mainInventory.getContainerSize();
        for (int i = start; i < end; i++) result.add(i);
        return result;
    }

    public boolean isToolSlot(int slot) {
        return slot == 0;
    }

    public boolean isContainerSlot(int slot) {
        return containerSlots().contains(slot);
    }

    public boolean isInventorySlot(int slot) {
        return inventorySlots().contains(slot);
    }

    public boolean isFloppySlot(int slot) {
        var stack = getItem(slot);
        if (stack.isEmpty()) return false;
        var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
        return driver != null && driver.slot(stack).equals(Slot.Floppy);
    }

    public boolean isUpgradeSlot(int slot) {
        return containerSlotType(slot).equals(Slot.Upgrade);
    }

    public String containerSlotType(int slot) {
        int idx = slot - 1;
        if (idx >= 0 && idx < info.containers.size()) {
            var stack = info.containers.get(idx);
            var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
            return driver instanceof li.cil.oc.api.driver.item.Container c ? c.providedSlot(stack) : Slot.None;
        }
        return Slot.None;
    }

    public int containerSlotTier(int slot) {
        int idx = slot - 1;
        if (idx >= 0 && idx < info.containers.size()) {
            var stack = info.containers.get(idx);
            var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
            return driver instanceof li.cil.oc.api.driver.item.Container c ? c.providedTier(stack) : Tier.None;
        }
        return Tier.None;
    }

    public static final ThreadLocal<RobotBase> movingRobot = new ThreadLocal<>();

    public boolean move(Direction direction) {
        var world = getLevel();
        if (world == null) return false;
        var bp = worldPosition;
        var newPos = bp.relative(direction);
        if (!world.hasChunk(newPos.getX() >> 4, newPos.getZ() >> 4)) return false;

        if (isServer()) {
            var delegate = EventHandlerDelegate.get();
            if (delegate != null && !delegate.postRobotMovePre(this, direction)) return false;
        }

        var robotBlock = li.cil.oc.api.Items.get(li.cil.oc.core.Constants.BlockName.Robot).block();
        var afterimageBlock = li.cil.oc.api.Items.get(li.cil.oc.core.Constants.BlockName.RobotAfterimage).block();
        if (robotBlock == null || afterimageBlock == null) return false;

        var newPosState = world.getBlockState(newPos);
        var newPosBlock = newPosState.getBlock();
        var wasAir = newPosState.isAir();

        try {
            movingRobot.set(this);
            world.setBlock(newPos, Blocks.AIR.defaultBlockState(), 3);
            boolean created = world.setBlock(newPos, world.getBlockState(bp), 1) &&
                    world.getBlockEntity(newPos) != null;
            if (created) {
                pendingOnLoadSkip = true;
                onProxyMoved(world, newPos, bp);
                world.setBlock(bp, Blocks.AIR.defaultBlockState(), 1);
                world.setBlock(bp, afterimageBlock.defaultBlockState(), 1);
                var moveTicks = Math.max((int) (OCSettings.get().moveDelay * 20), 1);
                setAnimateMove(BlockPosition.apply(bp.getX(), bp.getY(), bp.getZ(), world), moveTicks);
                if (isServer()) {
                    li.cil.oc.core.impl.common.PacketSender.sendRobotMove(this, BlockPosition.apply(bp.getX(), bp.getY(), bp.getZ(), world), direction);
                    world.sendBlockUpdated(newPos, world.getBlockState(newPos), world.getBlockState(newPos), 3);
                    world.sendBlockUpdated(bp, world.getBlockState(bp), world.getBlockState(bp), 3);
                    checkRedstoneInputChanged();
                    if (EventHandlerDelegate.get() != null)
                        EventHandlerDelegate.get().postRobotMovePost(this, direction);
                } else {
                    if (!wasAir && newPosBlock != Blocks.AIR && newPosBlock != afterimageBlock) {
                        FluidState fluidState = newPosState.getFluidState();
                        if (fluidState.isEmpty()) {
                            world.levelEvent(2001, newPos, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(newPosBlock));
                        } else {
                            float pitch = world.random.nextFloat() * 0.25f + 0.75f;
                            float volume = world.random.nextFloat() + 0.5f;
                            world.playSound(null, newPos.getX() + 0.5, newPos.getY() + 0.5, newPos.getZ() + 0.5,
                                    SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, volume, pitch);
                        }
                    }
                    world.sendBlockUpdated(bp, world.getBlockState(bp), world.getBlockState(bp), 3);
                    world.sendBlockUpdated(newPos, world.getBlockState(newPos), world.getBlockState(newPos), 3);
                }
                return true;
            } else {
                world.setBlock(newPos, Blocks.AIR.defaultBlockState(), 3);
            }
        } finally {
            movingRobot.remove();
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected void onProxyMoved(Level world, BlockPos newPos, BlockPos oldPos) {
    }

    public void checkRedstoneInputChanged() {
        if (isServer()) {
            shouldUpdateInput = true;
        }
    }

    public int[] input() {
        return input;
    }

    public int getInput(Direction side) {
        return Math.max(0, input[side.ordinal()]);
    }

    public void setInput(Direction side, int value) {
        input[side.ordinal()] = value;
    }

    public void setInput(int[] values) {
        System.arraycopy(values, 0, input, 0, Math.min(values.length, input.length));
    }

    public int[] output() {
        return output;
    }

    public int getOutput(Direction side) {
        return Math.max(0, output[side.ordinal()]);
    }

    public void setOutput(Direction side, int value) {
        output[side.ordinal()] = value;
    }

    public void setOutput(java.util.Map<?, ?> values) {
        for (var entry : values.entrySet()) {
            if (entry.getKey() instanceof Direction side && entry.getValue() instanceof Number number) {
                output[side.ordinal()] = number.intValue();
            }
        }
    }

    public int[][] bundledInput() {
        return bundledInput;
    }

    public int[][] bundledOutput() {
        return bundledOutput;
    }

    public boolean isOutputEnabled() {
        return isOutputEnabled;
    }

    public void setOutputEnabled(boolean value) {
        if (value != isOutputEnabled) {
            isOutputEnabled = value;
            if (!value) {
                java.util.Arrays.fill(output, 0);
            }
            onRedstoneOutputEnabledChanged();
        }
    }

    public boolean shouldUpdateInput() {
        return shouldUpdateInput;
    }

    public void shouldUpdateInput(boolean value) {
        shouldUpdateInput = value;
    }

    public void updateRedstoneInput(Direction side) {
        int oldValue = input[side.ordinal()];
        int newValue = li.cil.oc.core.impl.integration.util.BundledRedstone.computeInput(position(), side);
        if (oldValue != newValue) {
            input[side.ordinal()] = newValue;
            onRedstoneInputChanged(side.ordinal(), oldValue, newValue);
        }
        setBundledInput(side, li.cil.oc.core.impl.integration.util.BundledRedstone.computeBundledInput(position(), side));
    }

    public int maxInput() {
        int max = 0;
        for (int v : input) if (v > max) max = v;
        return max;
    }

    public void updateInventorySize() {
        synchronized (this) {
            int totalCapacity = 0;
            for (int i = 0; i < info.containers.size(); i++) {
                var stack = info.containers.get(i);
                if (!stack.isEmpty()) {
                    var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
                    if (driver instanceof li.cil.oc.api.driver.item.Inventory inv) {
                        totalCapacity += inv.inventoryCapacity(stack);
                    }
                }
            }
            for (int i = 0; i < componentCount(); i++) {
                int slot = componentSlots().stream().skip(i).findFirst().orElse(-1);
                if (slot >= 0) {
                    var stack = getItem(slot);
                    if (!stack.isEmpty()) {
                        var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
                        if (driver instanceof li.cil.oc.api.driver.item.Inventory inv) {
                            totalCapacity += inv.inventoryCapacity(stack);
                        }
                    }
                }
            }
            inventorySize = Math.min(maxInventorySize(), totalCapacity);
        }
    }

    public boolean isAnimatingMove() {
        return animationTicksLeft > 0 && (moveFromX != Integer.MAX_VALUE || moveFromY != Integer.MAX_VALUE || moveFromZ != Integer.MAX_VALUE);
    }

    public boolean isAnimatingSwing() {
        return animationTicksLeft > 0 && swingingTool;
    }

    public boolean isAnimatingTurn() {
        return animationTicksLeft > 0 && turnAxis != 0;
    }

    public void animateSwing(double duration) {
        setAnimateSwing((int) (duration * 20));
        PacketSender.sendRobotAnimateSwing(this, animationTicksTotal);
    }

    public void animateTurn(boolean clockwise, double duration) {
        setAnimateTurn(clockwise ? 1 : -1, (int) (duration * 20));
        PacketSender.sendRobotAnimateTurn(this, (byte) turnAxis, animationTicksTotal);
    }

    public void setAnimateMove(BlockPosition from, int ticks) {
        if (isAnimatingMove() && moveFromX == from.x() && moveFromY == from.y() && moveFromZ == from.z()) {
            return;
        }
        animationTicksTotal = ticks + 2;
        prepareForAnimation();
        moveFromX = from.x();
        moveFromY = from.y();
        moveFromZ = from.z();
    }

    public void setAnimateSwing(int ticks) {
        animationTicksTotal = Math.max(ticks, 5);
        prepareForAnimation();
        swingingTool = true;
    }

    public void setAnimateTurn(int axis, int ticks) {
        animationTicksTotal = ticks;
        prepareForAnimation();
        turnAxis = axis;
    }

    private void prepareForAnimation() {
        animationTicksLeft = animationTicksTotal;
        moveFromX = Integer.MAX_VALUE;
        moveFromY = Integer.MAX_VALUE;
        moveFromZ = Integer.MAX_VALUE;
        swingingTool = false;
        turnAxis = 0;
    }

    public void updateEntity() {
        super.updateEntity();
        if (animationTicksLeft > 0) {
            animationTicksLeft--;
            if (animationTicksLeft == 0) {
                moveFromX = Integer.MAX_VALUE;
                moveFromY = Integer.MAX_VALUE;
                moveFromZ = Integer.MAX_VALUE;
                swingingTool = false;
                turnAxis = 0;
            }
        }
        if (changeScheduled) {
            setChanged();
            changeScheduled = false;
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
        if (isServer()) {
            if (node != null && node.network() != null) {
                machine.update();
                if (this instanceof li.cil.oc.core.impl.common.inventory.ComponentInventory ci) {
                    ci.updateComponents();
                }
            }
            boolean running = machine.isRunning();
            boolean errored = machine.lastError() != null;
            if (isRunning != running || hasErrored != errored) {
                isRunning = running;
                hasErrored = errored;
                setChanged();
                li.cil.oc.core.impl.common.PacketSender.sendComputerState(this, isRunning, errored);
                if (li.cil.oc.core.impl.util.EventHandlerDelegate.get() != null) {
                    if (running) li.cil.oc.core.impl.util.EventHandlerDelegate.get().onRobotStart(this);
                    else li.cil.oc.core.impl.util.EventHandlerDelegate.get().onRobotStopped(this);
                }
            }
            var level = getLevel();
            if (level != null && level.getGameTime() % OCSettings.get().tickFrequency == 0) {
                var energyNode = bot instanceof li.cil.oc.core.impl.server.component.Robot rb ? rb.node() : node();
                var connector = (li.cil.oc.api.network.Connector) energyNode;
                if (info.tier == 3)
                    connector.changeBuffer(Double.POSITIVE_INFINITY);
                globalBuffer = connector.globalBuffer();
                globalBufferSize = connector.globalBufferSize();
                info.totalEnergy = (int) globalBuffer;
                info.robotEnergy = (int) connector.localBuffer();
            }
            if (!appliedToolEnchantments) {
                appliedToolEnchantments = true;
                var tool = getItem(0);
                if (!tool.isEmpty()) {
                    var modMap = ArrayListMultimap.<Holder<Attribute>, AttributeModifier>create();
                    tool.forEachModifier(EquipmentSlot.MAINHAND, modMap::put);
                    player().getAttributes().addTransientAttributeModifiers(modMap);
                }
            }
            if (shouldUpdateInput) {
                shouldUpdateInput = false;
                try {
                    for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                        updateRedstoneInput(side);
                    }
                } catch (Throwable t) {
                    LOGGER.warn("Error updating redstone input for robot at {}: {}", worldPosition, t.toString());
                }
            }
        }
    }

    @Override
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public void selectedSlot(int value) {
        selectedSlot = Math.clamp(value, 0, Math.max(0, mainInventory.getContainerSize() - 1));
        PacketSender.sendRobotSelectedSlotChange(this, selectedSlot());
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public void selectedTank(int value) {
        selectedTank = value;
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        readFromNBTForServer(nbt, getEffectiveProvider());
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        writeToNBTForServer(nbt, getEffectiveProvider());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        readFromNBTForClient(nbt, getEffectiveProvider());
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        writeToNBTForClient(nbt, getEffectiveProvider());
    }

    public void readFromNBTForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        if (node != null && nbt.contains(OCSettings.namespace + "node")) {
            node.load(nbt.getCompound(OCSettings.namespace + "node"), provider);
        }
        if (nbt.contains(OCSettings.namespace + "owner")) ownerName = nbt.getString(OCSettings.namespace + "owner");
        if (nbt.contains(OCSettings.namespace + "ownerUuid"))
            ownerUUID = UUID.fromString(nbt.getString(OCSettings.namespace + "ownerUuid"));
        if (inventorySize > 0)
            selectedSlot = Math.clamp(nbt.getInt(OCSettings.namespace + "selectedSlot"), 0, Math.max(0, mainInventory.getContainerSize() - 1));
        selectedTank = nbt.getInt(OCSettings.namespace + "selectedTank");
        animationTicksTotal = nbt.getInt(OCSettings.namespace + "animationTicksTotal");
        animationTicksLeft = nbt.getInt(OCSettings.namespace + "animationTicksLeft");
        if (animationTicksLeft > 0) {
            moveFromX = nbt.getInt(OCSettings.namespace + "moveFromX");
            moveFromY = nbt.getInt(OCSettings.namespace + "moveFromY");
            moveFromZ = nbt.getInt(OCSettings.namespace + "moveFromZ");
            swingingTool = nbt.getBoolean(OCSettings.namespace + "swingingTool");
            turnAxis = nbt.getByte(OCSettings.namespace + "turnAxis");
        }

        if (nbt.contains(OCSettings.namespace + "inventory")) {
            var invList = nbt.getList(OCSettings.namespace + "inventory", 10);
            Arrays.fill(inventory, ItemStack.EMPTY);
            for (int i = 0; i < invList.size(); i++) {
                var tag = invList.getCompound(i);
                int slot = tag.getByte("slot") & 0xFF;
                if (slot < inventory.length && provider != null) {
                    inventory[slot] = ItemStack.parseOptional(provider, tag.getCompound("stack"));
                }
            }
        }

        if (nbt.contains(OCSettings.namespace + "input")) {
            int[] savedInput = nbt.getIntArray(OCSettings.namespace + "input");
            System.arraycopy(savedInput, 0, input, 0, Math.min(savedInput.length, input.length));
        }
        if (nbt.contains(OCSettings.namespace + "output")) {
            int[] savedOutput = nbt.getIntArray(OCSettings.namespace + "output");
            System.arraycopy(savedOutput, 0, output, 0, Math.min(savedOutput.length, output.length));
        }
        readBundledNBT(nbt, OCSettings.namespace + "rs.bundledInput", bundledInput);
        readBundledNBT(nbt, OCSettings.namespace + "rs.bundledOutput", bundledOutput);
        if (nbt.contains(OCSettings.namespace + "facing"))
            facing = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "facing"));
        isRunning = nbt.getBoolean(OCSettings.namespace + "isRunning");
        isOutputEnabled = nbt.getBoolean(OCSettings.namespace + "isOutputEnabled");
    }

    public void writeToNBTForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (this) {
            info.save(nbt, provider);
            if (node != null) {
                li.cil.oc.core.impl.util.ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "node", t -> node.save(t, provider));
            }
            nbt.putString(OCSettings.namespace + "owner", ownerName);
            nbt.putString(OCSettings.namespace + "ownerUuid", ownerUUID.toString());
            nbt.putInt(OCSettings.namespace + "selectedSlot", selectedSlot);
            nbt.putInt(OCSettings.namespace + "selectedTank", selectedTank);
            if (isAnimatingMove() || isAnimatingSwing() || isAnimatingTurn()) {
                nbt.putInt(OCSettings.namespace + "animationTicksTotal", animationTicksTotal);
                nbt.putInt(OCSettings.namespace + "animationTicksLeft", animationTicksLeft);
                nbt.putInt(OCSettings.namespace + "moveFromX", moveFromX);
                nbt.putInt(OCSettings.namespace + "moveFromY", moveFromY);
                nbt.putInt(OCSettings.namespace + "moveFromZ", moveFromZ);
                nbt.putBoolean(OCSettings.namespace + "swingingTool", swingingTool);
                nbt.putByte(OCSettings.namespace + "turnAxis", (byte) turnAxis);
            }

            var invList = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < inventory.length; i++) {
                if (!inventory[i].isEmpty() && provider != null) {
                    var tag = new CompoundTag();
                    tag.putByte("slot", (byte) i);
                    tag.put("stack", inventory[i].save(provider, new CompoundTag()));
                    invList.add(tag);
                }
            }
            nbt.put(OCSettings.namespace + "inventory", invList);

            nbt.putIntArray(OCSettings.namespace + "input", input);
            nbt.putIntArray(OCSettings.namespace + "output", output);
            writeBundledNBT(nbt, OCSettings.namespace + "rs.bundledInput", bundledInput);
            writeBundledNBT(nbt, OCSettings.namespace + "rs.bundledOutput", bundledOutput);
            nbt.putInt(OCSettings.namespace + "facing", facing.get3DDataValue());
            nbt.putBoolean(OCSettings.namespace + "isRunning", isRunning);
            nbt.putBoolean(OCSettings.namespace + "isOutputEnabled", isOutputEnabled);
        }
    }

    public void readFromNBTForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        info.load(nbt, provider);
        updateInventorySize();
        selectedSlot = nbt.getInt("selectedSlot");
        isRunning = nbt.getBoolean("isRunning");
        int nbtTicksTotal = nbt.getInt("animationTicksTotal");
        int nbtTicksLeft = nbt.getInt("animationTicksLeft");
        boolean sameMove = animationTicksLeft > 0 && nbtTicksLeft > 0 && nbt.contains("moveFromX")
            && moveFromX == nbt.getInt("moveFromX")
            && moveFromY == nbt.getInt("moveFromY")
            && moveFromZ == nbt.getInt("moveFromZ");
        if (!sameMove) {
            animationTicksTotal = nbtTicksTotal;
            animationTicksLeft = nbtTicksLeft;
            if (nbtTicksLeft > 0) {
                if (nbt.contains("moveFromX")) {
                    moveFromX = nbt.getInt("moveFromX");
                    moveFromY = nbt.getInt("moveFromY");
                    moveFromZ = nbt.getInt("moveFromZ");
                }
                swingingTool = nbt.getBoolean("swingingTool");
                turnAxis = nbt.getByte("turnAxis");
            } else {
                moveFromX = Integer.MAX_VALUE;
                moveFromY = Integer.MAX_VALUE;
                moveFromZ = Integer.MAX_VALUE;
            }
        }
        if (nbt.contains(OCSettings.namespace + "inventory")) {
            var invList = nbt.getList(OCSettings.namespace + "inventory", 10);
            Arrays.fill(inventory, ItemStack.EMPTY);
            for (int i = 0; i < invList.size(); i++) {
                var tag = invList.getCompound(i);
                int slot = tag.getByte("slot") & 0xFF;
                if (slot < inventory.length) {
                    if (provider != null) {
                        inventory[slot] = ItemStack.parseOptional(provider, tag.getCompound("stack"));
                    }
                }
            }
        }
    }

    public void writeToNBTForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (this) {
            info.save(nbt, provider);
            nbt.putInt("selectedSlot", selectedSlot);
            nbt.putBoolean("isRunning", isRunning);
            if (isAnimatingMove() || isAnimatingSwing() || isAnimatingTurn()) {
                nbt.putInt("animationTicksTotal", animationTicksTotal);
                nbt.putInt("animationTicksLeft", animationTicksLeft);
                nbt.putInt("moveFromX", moveFromX);
                nbt.putInt("moveFromY", moveFromY);
                nbt.putInt("moveFromZ", moveFromZ);
                nbt.putBoolean("swingingTool", swingingTool);
                nbt.putByte("turnAxis", (byte) turnAxis);
            }
            if (provider != null) {
                var invList = new net.minecraft.nbt.ListTag();
                for (int i = 0; i < inventory.length; i++) {
                    if (!inventory[i].isEmpty()) {
                        var tag = new CompoundTag();
                        tag.putByte("slot", (byte) i);
                        tag.put("stack", inventory[i].save(provider, new CompoundTag()));
                        invList.add(tag);
                    }
                }
                nbt.put(OCSettings.namespace + "inventory", invList);
            }
        }
    }

    @SuppressWarnings("unused")
    public abstract boolean hasRedstoneCard();

    @SuppressWarnings("unused")
    public abstract java.util.List<li.cil.oc.api.network.Environment> agentComponents();

    @Override
    public Machine machine() {
        return machine;
    }

    @Override
    public Container equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public Container mainInventory() {
        return mainInventory;
    }

    @Override
    public void setSelectedSlot(int index) {
        selectedSlot(index);
    }

    @Override
    public void setSelectedTank(int index) {
        selectedTank(index);
    }

    @Override
    public String name() {
        return info.name;
    }

    @Override
    public void setName(String name) {
        info.name = name;
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
    }

    @Override
    public Level level() {
        return getLevel();
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

    @Override
    public abstract double xPosition();

    @Override
    public abstract double yPosition();

    @Override
    public abstract double zPosition();

    private boolean changeScheduled = false;

    @Override
    public void markChanged() {
        changeScheduled = true;
        renderingErrored = false;
    }

    @Override
    public boolean isConnected() {
        return node != null && node.address() != null && node.network() != null;
    }

    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return info.components;
    }

    @Override
    public int componentSlot(String ignoredAddress) {
        return -1;
    }

    @Override
    public void onMachineConnect(Node node) {
        if (node == node()) {
            if (bot instanceof li.cil.oc.core.impl.server.component.Robot rb) {
                node.connect(rb.node());
            }
            if (node instanceof li.cil.oc.api.network.Connector c) {
                c.setLocalBufferSize(0);
            }
        }
    }

    @Override
    public void onMachineDisconnect(Node node) {
        if (node == node()) {
            if (bot instanceof li.cil.oc.core.impl.server.component.Robot rb) {
                rb.node().remove();
            }
        }
    }

    @Override
    public void onConnect(Node ignoredNode) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message ignoredMessage) {
    }

    public void dropSlot(int slot) {
        li.cil.oc.core.impl.util.InventoryUtils.dropSlot(BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), this, slot, getMaxStackSize());
    }

    public void dropSlot(int slot, int count, Direction direction) {
        li.cil.oc.core.impl.util.InventoryUtils.dropSlot(BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), this, slot, count, direction);
    }

    public void dropAllSlots() {
        li.cil.oc.core.impl.util.InventoryUtils.dropAllSlots(BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), this);
    }

    public void dropInventorySlots() {
        var position = BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel());
        var invSlots = inventorySlots();
        for (int slot : invSlots) {
            li.cil.oc.core.impl.util.InventoryUtils.dropSlot(position, this, slot, getMaxStackSize());
        }
    }

    public void spawnStackInWorld(ItemStack stack) {
        spawnStackInWorld(stack, null);
    }

    public void spawnStackInWorld(ItemStack stack, Direction direction) {
        li.cil.oc.core.impl.util.InventoryUtils.spawnStackInWorld(BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), stack, direction, null);
    }

    public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        var states = EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
        if (machine().isRunning()) states.add(li.cil.oc.api.util.StateAware.State.IsWorking);
        return states;
    }

    @Override
    public Direction facing() {
        return facing;
    }

    public void facing(Direction value) {
        facing = value;
    }

    public void syncFromBlockState(BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
    }

    @Override
    public Direction toGlobal(Direction value) {
        return RotationHelper.toGlobal(Direction.NORTH, facing, value);
    }

    @Override
    public Direction toLocal(Direction value) {
        return RotationHelper.toLocal(Direction.NORTH, facing, value);
    }


    @Override
    public int getContainerSize() {
        return equipmentInventory.getContainerSize() + mainInventory.getContainerSize() + componentCount();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        int size = getContainerSize();
        if (slot < 0 || slot >= size) return ItemStack.EMPTY;
        int compCount = componentCount();
        if (slot >= size - compCount) {
            int compIdx = slot - (size - compCount);
            if (compIdx >= 0 && compIdx < info.components.size()) {
                return info.components.get(compIdx);
            }
            return ItemStack.EMPTY;
        }
        if (slot < inventory.length) {
            var stack = inventory[slot];
            return stack != null ? stack : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        var stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.getCount() <= amount) {
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }
        return stack.split(amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        var stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        int size = getContainerSize();
        if (slot < 0 || slot >= size) return;
        int compCount = componentCount();
        if (slot >= size - compCount) {
            int compIdx = slot - (size - compCount);
            if (compIdx >= 0 && compIdx < info.components.size()) {
                info.components.set(compIdx, stack);
            }
        } else if (slot < inventory.length) {
            inventory[slot] = stack;
        }
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        double d = player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        return d <= 64;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) return true;
        if (isContainerSlot(slot)) {
            var driver = hostClass != null ? li.cil.oc.api.API.driver.driverFor(stack, hostClass) : null;
            if (driver != null) {
                String slotType = containerSlotType(slot);
                return driver.slot(stack).equals(slotType) && driver.tier(stack) <= containerSlotTier(slot);
            }
            return false;
        }
        return true;
    }

    @Override
    public void clearContent() {
        Arrays.fill(inventory, ItemStack.EMPTY);
        Collections.fill(info.components, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void startOpen(@NotNull Player player) {
    }

    @Override
    public void stopOpen(@NotNull Player player) {
    }

    public void dispose() {
        if (isClient()) {
            onClientDispose();
        } else {
            if (EventHandlerDelegate.get() != null) EventHandlerDelegate.get().onRobotStopped(this);
        }
    }

    protected abstract void onClientDispose();

    private static void readBundledNBT(CompoundTag nbt, String key, int[][] target) {
        if (nbt.contains(key, 11)) {
            var list = nbt.getList(key, 11);
            for (int i = 0; i < list.size() && i < target.length; i++) {
                int[] arr = list.getIntArray(i);
                System.arraycopy(arr, 0, target[i], 0, Math.min(arr.length, target[i].length));
            }
        }
    }

    private static void writeBundledNBT(CompoundTag nbt, String key, int[][] source) {
        var list = new net.minecraft.nbt.ListTag();
        for (int[] arr : source) {
            list.add(new net.minecraft.nbt.IntArrayTag(arr));
        }
        nbt.put(key, list);
    }
}
