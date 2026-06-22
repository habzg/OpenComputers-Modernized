package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.common.tileentity.traits.ComponentInventory;
import li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.tileentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.integration.util.ItemCharge;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.util.RobotChargeableFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Charger extends TileEntity implements li.cil.oc.api.network.Environment, PowerAcceptor, RedstoneAware, Rotatable, ComponentInventory, Analyzable, li.cil.oc.api.util.StateAware, DeviceInfo {
    public static BlockEntityType<Charger> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.None)
            .withConnector(Settings.get().bufferConverter)
            .create();
    public final Set<Chargeable> connectors = new HashSet<>();
    public final Set<ItemStack> equipment = new HashSet<>();
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
    private final int[] _output = new int[]{0, 0, 0, 0, 0, 0};
    private boolean _isOutputEnabled = false;
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Charger",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "PowerUpper"
    );
    public double chargeSpeed = 0;
    public boolean hasPower = false;
    public boolean invertSignal = false;
    private Direction facing = Direction.SOUTH;
    private final ItemStack[] _items = new ItemStack[getContainerSize()];
    private ManagedEnvironment[] _components;

    public Charger(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public Level level() {
        return getLevel();
    }

    public double xPosition() {
        return worldPosition.getX() + 0.5;
    }

    public double yPosition() {
        return worldPosition.getY() + 0.5;
    }

    public double zPosition() {
        return worldPosition.getZ() + 0.5;
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
    public ManagedEnvironment[] _components() {
        return _components;
    }

    @Override
    public void _components(ManagedEnvironment[] value) {
        _components = value;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
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

    public void markChanged() {
    }

    public boolean isConnected() {
        return node.address() != null && node.network() != null;
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public li.cil.oc.api.network.EnvironmentHost host() {
        return this;
    }

    private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();

    @Override
    public ArrayList<ManagedEnvironment> updatingComponents() {
        return updatingComponents;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer() && getLevel() != null) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            connectComponents();
            onNeighborChanged();
        }
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
    }

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
    public boolean canConnectPower(Direction side) {
        return hasConnector(side);
    }

    protected boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public Direction facing() {
        var state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        return facing;
    }

    @Override
    public void facing(Direction value) {
        this.facing = value;
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
        ae2OnNeighborChanged();
    }

    @Override
    public double energyThroughput() {
        return Settings.get().chargerRate;
    }

    @Override
    public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        if (!connectors.isEmpty()) {
            if (hasPower) return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
            else return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.CanWork);
        }
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.chargerspeed", (int) (chargeSpeed * 100) + "%"), false);
        return null;
    }

    @Override
    public int[] input() {
        return _input;
    }

    @Override
    public int[] output() {
        return _output;
    }

    @Override
    public boolean isOutputEnabled() {
        return _isOutputEnabled;
    }

    @Override
    public void setOutputEnabled(boolean value) {
        _isOutputEnabled = value;
        if (!value) {
            Arrays.fill(_output, 0);
        }
    }

    @Override
    public void setInput(Direction side, int value) {
        _input[side.ordinal()] = value;
    }

    @Override
    public void setInput(int[] values) {
        for (int i = 0; i < values.length && i < _input.length; i++) _input[i] = values[i];
    }

    @Override
    public int maxInput() {
        int max = 0;
        for (int v : _input) if (v > max) max = v;
        return max;
    }

    @Override
    public int getOutput(Direction side) {
        return _output[side.ordinal()];
    }

    @Override
    public void setOutput(Direction side, int value) {
        _output[side.ordinal()] = value;
    }

    @Override
    public int getInput(Direction side) {
        return Math.max(_input[side.ordinal()], 0);
    }

    private void chargeStack(ItemStack stack, double charge) {
        if (stack != null && charge > 0) {
            double offered = charge + ((li.cil.oc.api.network.Connector) node).changeBuffer(-charge);
            double surplus = ItemCharge.charge(stack, offered);
            ((li.cil.oc.api.network.Connector) node).changeBuffer(surplus);
        }
    }

    @Override
    public void checkRedstoneInputChanged() {
        if (getLevel() == null) return;
        for (var side : Direction.values()) {
            try {
                var signal = Math.max(getLevel().getSignal(worldPosition, side), getLevel().getBestNeighborSignal(worldPosition));
                setInput(side, signal);
            } catch (Exception ignored) {
            }
        }
        var signal = java.util.Arrays.stream(input()).max().orElse(0);
        chargeSpeed = invertSignal ? (15 - signal) / 15.0 : signal / 15.0;
        if (isServer()) {
            PacketSender.sendChargerState(this, chargeSpeed, hasPower);
        }
    }

    @Override
    public void validate() {
        if (isServer()) {
            for (var side : Direction.values()) {
                updateRedstoneInput(side);
            }
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if ((level().getGameTime() + Math.abs(hashCode())) % 20 == 0) updateConnectors();
        if (isServer() && level().getGameTime() % Settings.get().tickFrequency == 0) {
            boolean canCharge = Settings.get().ignorePower;
            double charge = Settings.get().chargeRateExternal * chargeSpeed * Settings.get().tickFrequency;
            canCharge = canCharge || (charge > 0 && ((li.cil.oc.api.network.Connector) node).globalBuffer() >= charge * 0.5);
            if (canCharge) {
                for (var connector : connectors) {
                    var c = (li.cil.oc.api.network.Connector) node;
                    c.changeBuffer(connector.changeBuffer(charge + c.changeBuffer(-charge)));
                }
            }
            charge = Settings.get().chargeRateTablet * chargeSpeed * Settings.get().tickFrequency;
            canCharge = canCharge || (charge > 0 && ((li.cil.oc.api.network.Connector) node).globalBuffer() >= charge * 0.5);
            if (canCharge) {
                for (int i = 0; i < getContainerSize(); i++) chargeStack(getItem(i), charge);
            }
            canCharge = canCharge || (charge > 0 && ((li.cil.oc.api.network.Connector) node).globalBuffer() >= charge * 0.5);
            if (canCharge) {
                for (var stack : equipment) chargeStack(stack, charge);
            }
            if (hasPower && !canCharge) {
                hasPower = false;
                PacketSender.sendChargerState(this, chargeSpeed, false);
            }
            if (!hasPower && canCharge) {
                hasPower = true;
                PacketSender.sendChargerState(this, chargeSpeed, true);
            }
        }
        if (isClient() && chargeSpeed > 0 && hasPower && level().getGameTime() % 10 == 0) {
            for (var connector : connectors) {
                var pos = connector.pos();
                double theta = level().random.nextDouble() * Math.PI;
                double phi = level().random.nextDouble() * Math.PI * 2;
                double dx = 0.45 * Math.sin(theta) * Math.cos(phi);
                double dy = 0.45 * Math.sin(theta) * Math.sin(phi);
                double dz = 0.45 * Math.cos(theta);
                level().addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.x + dx, pos.y + dz, pos.z + dy, 0, 0, 0);
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        var loadProvider = getEffectiveProvider();
        if (loadProvider != null) load(nbt, loadProvider);
        if (nbt.contains(Settings.namespace + "chargeSpeed"))
            chargeSpeed = Math.clamp(nbt.getDouble(Settings.namespace + "chargeSpeed"), 0, 1);
        else
            chargeSpeed = Math.clamp(nbt.getDouble("chargeSpeed"), 0, 1);
        if (nbt.contains(Settings.namespace + "hasPower"))
            hasPower = nbt.getBoolean(Settings.namespace + "hasPower");
        else
            hasPower = nbt.getBoolean("hasPower");
        if (nbt.contains(Settings.namespace + "invertSignal"))
            invertSignal = nbt.getBoolean(Settings.namespace + "invertSignal");
        else
            invertSignal = nbt.getBoolean("invertSignal");
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        var provider = getEffectiveProvider();
        if (provider != null) save(nbt, provider);
        nbt.putDouble(Settings.namespace + "chargeSpeed", chargeSpeed);
        nbt.putBoolean(Settings.namespace + "hasPower", hasPower);
        nbt.putBoolean(Settings.namespace + "invertSignal", invertSignal);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        chargeSpeed = nbt.getDouble(Settings.namespace + "chargeSpeed");
        hasPower = nbt.getBoolean(Settings.namespace + "hasPower");
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        nbt.putDouble(Settings.namespace + "chargeSpeed", chargeSpeed);
        nbt.putBoolean(Settings.namespace + "hasPower", hasPower);
    }

    public boolean isComponentSlot(int slot, ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
        return driver != null && driver.slot(stack).equals(Slot.Tablet);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
            if (driver != null && driver.slot(stack).equals(Slot.Tablet)) return true;
        }
        return ItemCharge.canCharge(stack);
    }

    @Override
    public void updateRedstoneInput(Direction side) {
        if (getLevel() == null) return;
        int oldValue = _input[side.ordinal()];
        int newValue = Math.max(getLevel().getSignal(worldPosition, side), getLevel().getBestNeighborSignal(worldPosition));
        if (oldValue != newValue) {
            _input[side.ordinal()] = newValue;
        }
        int signal = java.util.Arrays.stream(input()).max().orElse(0);
        chargeSpeed = invertSignal ? (15 - signal) / 15.0 : signal / 15.0;
        if (isServer()) {
            PacketSender.sendChargerState(this, chargeSpeed, hasPower);
        }
    }

    public void onNeighborChanged() {
        checkRedstoneInputChanged();
        updateConnectors();
        ae2OnNeighborChanged();
    }

    public void updateConnectors() {
        var robots = new java.util.ArrayList<Chargeable>();
        for (var side : Direction.values()) {
            var offsetPos = BlockPosition.apply(this).offset(side);
            var blockPos = new BlockPos(offsetPos.x(), offsetPos.y(), offsetPos.z());
            if (level().hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                var te = level().getBlockEntity(blockPos);
                var rc = RobotChargeableFactory.get() != null ? (Chargeable) RobotChargeableFactory.get().tryCreate(te) : null;
                if (rc != null) robots.add(rc);
            }
        }
        AABB bounds = BlockPosition.apply(this).bounds().inflate(1);
        var drones = level().getEntitiesOfClass(Drone.class, bounds);
        var robotChargeables = new java.util.ArrayList<Chargeable>();
        for (var drone : drones) robotChargeables.add(new DroneChargeable(drone));
        var players = level().getEntitiesOfClass(Player.class, bounds);
        var chargeablePlayers = new java.util.ArrayList<Chargeable>();
        for (var player : players) {
            if (li.cil.oc.api.Nanomachines.hasController(player)) chargeablePlayers.add(new PlayerChargeable(player));
        }
        var newConnectors = new java.util.HashSet<Chargeable>();
        newConnectors.addAll(robots);
        newConnectors.addAll(robotChargeables);
        newConnectors.addAll(chargeablePlayers);
        if (!connectors.equals(newConnectors)) {
            connectors.clear();
            connectors.addAll(newConnectors);
            level().updateNeighborsAt(worldPosition, block());
        }
        equipment.clear();
        for (var player : players) {
            for (var stack : player.getInventory().items) {
                if (stack != null && !stack.isEmpty()) {
                    var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
                    if ((driver != null && driver.slot(stack).equals(Slot.Tablet)) || ItemCharge.canCharge(stack)) {
                        equipment.add(stack);
                    }
                }
            }
        }
    }

    public interface Chargeable {
        Vec3 pos();

        double changeBuffer(double delta);
    }

    public abstract static class ConnectorChargeable implements Chargeable {
        protected final li.cil.oc.api.network.Connector connector;

        public ConnectorChargeable(li.cil.oc.api.network.Connector connector) {
            this.connector = connector;
        }

        @Override
        public double changeBuffer(double delta) {
            return connector.changeBuffer(delta);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ConnectorChargeable cc) return cc.connector == connector;
            return false;
        }
    }

    public static class DroneChargeable extends ConnectorChargeable {
        public final Drone drone;

        public DroneChargeable(Drone drone) {
            super((li.cil.oc.api.network.Connector) drone.components.node());
            this.drone = drone;
        }

        @Override
        public Vec3 pos() {
            return drone.position();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DroneChargeable dc) return dc.drone == drone;
            return false;
        }

        @Override
        public int hashCode() {
            return drone.hashCode();
        }
    }

    public record PlayerChargeable(Player player) implements Chargeable {

        @Override
        public Vec3 pos() {
            return player.position();
        }

        @Override
        public double changeBuffer(double delta) {
            var controller = li.cil.oc.api.Nanomachines.getController(player);
            if (controller != null) return controller.changeBuffer(delta);
            return delta;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PlayerChargeable(Player player1)) return player1 == player;
            return false;
        }

    }
}
