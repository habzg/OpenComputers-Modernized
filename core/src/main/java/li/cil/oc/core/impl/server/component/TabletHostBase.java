package li.cil.oc.core.impl.server.component;

import java.util.ArrayList;
import java.util.Map;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.inventory.ComponentInventory;
import li.cil.oc.core.impl.common.item.TabletWrapper;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class TabletHostBase implements ComponentInventory, MachineHost, Environment, li.cil.oc.api.internal.Tablet, TabletWrapper, DeviceInfo {
    private final Map<String, String> deviceInfo;
    private ManagedEnvironment[] _components;
    private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();
    private boolean isInitialized = false;
    private boolean lastRunning = false;
    private li.cil.oc.api.machine.Machine _machine;
    private Tablet _tabletComponent;
    public Level creationLevel;

    protected TabletHostBase() {
        deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Tablet",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Jogger");
    }

    public abstract ItemStack getStack();

    protected abstract CompoundTag loadMachineTag();

    protected abstract void saveMachineTag(CompoundTag nbt);

    public abstract String containerSlotType();

    public abstract int containerSlotTier();

    @Override
    public li.cil.oc.api.machine.Machine machine() {
        if (_machine == null) {
            level();
            _machine = Machine.create(this);
            if (_machine != null && !level().isClientSide()) {
                var tag = loadMachineTag();
                if (!tag.isEmpty()) {
                    var provider = level().registryAccess();
                    try {
                        _machine.load(tag, provider);
                    } catch (Throwable e) {
                        ComponentInventory.LOGGER.warn("Failed to load tablet machine data.", e);
                    }
                }
                var node = _machine.node();
                if (node != null && node.network() == null) {
                    li.cil.oc.api.Network.joinNewNetwork(node);
                }
                var tabletComponent = serverComponent();
                if (tabletComponent != null) {
                    var tabletData = new li.cil.oc.core.impl.common.item.data.TabletData(getStack());
                    if (tabletComponent.node instanceof Connector connector) {
                        var charge = Math.max(0, tabletData.energy - connector.localBuffer());
                        connector.changeBuffer(charge);
                    }
                }
                persistMachineState();
            }
        }
        return _machine;
    }

    public void persistMachineState() {
        if (_machine != null) {
            level();
            if (!level().isClientSide() && _machine.node() != null) {
                var tag = new CompoundTag();
                _machine.save(tag, level().registryAccess());
                saveMachineTag(tag);
            }
        }
    }

    public @Nullable Tablet serverComponent() {
        if (_tabletComponent == null) {
            level();
            if (!level().isClientSide()) {
                _tabletComponent = new Tablet(this);
            }
        }
        return _tabletComponent;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public Node node() {
        var m = machine();
        return m.node();
    }

    @Override
    public void onConnect(Node node) {
        if (node == node()) {
            connectComponents();
            var c = serverComponent();
            if (c != null) node.connect(c.node);
        } else if (node.host() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.setMaximumColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.FourBit);
            buffer.setMaximumResolution(80, 25);
        }
    }

    @Override
    public void connectItemNode(Node node) {
        ComponentInventory.super.connectItemNode(node);
        if (node == null) return;
        if (node.host() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            for (ManagedEnvironment env : componentEnvironments()) {
                if (env instanceof li.cil.oc.api.internal.Keyboard keyboard) {
                    buffer.node().connect(keyboard.node());
                }
            }
        } else if (node.host() instanceof li.cil.oc.api.internal.Keyboard keyboard) {
            for (ManagedEnvironment env : componentEnvironments()) {
                if (env instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                    keyboard.node().connect(buffer.node());
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == node()) {
            disconnectComponents();
            var c = serverComponent();
            if (c != null) c.node.remove();
        }
    }

    @Override
    public void onMessage(Message message) {
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
        return updatingComponents;
    }

    @Override
    public EnvironmentHost host() {
        return this;
    }

    @Override
    public boolean isSizeInventoryReady() {
        return true;
    }

    @Override
    public void onMachineConnect(Node node) {
        onConnect(node);
    }

    @Override
    public void onMachineDisconnect(Node node) {
        onDisconnect(node);
    }

    @Override
    public void setChanged() {
        if (!level().isClientSide()) {
            new li.cil.oc.core.impl.common.item.data.TabletData(getStack()).save(getStack());
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {}

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        saveComponents(provider);
        persistMachineState();
        new li.cil.oc.core.impl.common.item.data.TabletData(getStack()).save(getStack());
    }

    public void update() {
        var level = level();
        if (!isInitialized) {
            isInitialized = true;
            connectComponents();
            if (level.isClientSide) {
                for (ManagedEnvironment env : componentEnvironments()) {
                    if (env instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                        buffer.setMaximumColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.FourBit);
                        buffer.setMaximumResolution(80, 25);
                    }
                }
            }
        }
        if (!level.isClientSide) {
            var m = machine();
            if (isCreative() && level.getGameTime() % (long) OCSettings.get().tickFrequency == 0) {
                if (m.node() instanceof Connector conn) {
                    conn.changeBuffer(Double.POSITIVE_INFINITY);
                }
            }
            m.update();
            updateComponents();
            var data = new li.cil.oc.core.impl.common.item.data.TabletData(getStack());
            var running = m.isRunning();
            data.isRunning = running;
            var c = serverComponent();
            if (c != null && c.node instanceof Connector connector) {
                data.energy = connector.localBuffer();
                data.maxEnergy = connector.localBufferSize();
            }
            data.save(getStack());
            if (player() != null) {
                player().getInventory().setChanged();
            }
            if (lastRunning != running) {
                lastRunning = running;
                persistMachineState();
                if (player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                    li.cil.oc.core.impl.common.PacketSender.sendMachineItemState(sp, getStack(), running);
                }
                if (running) {
                    for (ManagedEnvironment env : componentEnvironments()) {
                        if (env instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                            buffer.setPowerState(true);
                        }
                    }
                }
            }
        }
    }

    public void startMachine() {
        var m = machine();
        if (!m.isRunning()) {
            m.start();
            var lastError = m.lastError();
            if (lastError != null) {
                var p = player();
                if (p != null) {
                    p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.lasterror", net.minecraft.network.chat.Component.translatable(lastError)));
                }
            }
        }
    }

    public void stopMachine() {
        var m = machine();
        if (m.isRunning()) {
            m.stop();
        }
    }

    @SuppressWarnings("unused")
    public void player(Player p) {
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    public boolean isCreative() {
        var data = new li.cil.oc.core.impl.common.item.data.TabletData(getStack());
        return data.tier == li.cil.oc.core.common.Tier.Four;
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        var items = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < getContainerSize(); i++) {
            var stack = getItem(i);
            if (!stack.isEmpty() && isComponentSlot(i, stack)) {
                items.add(stack);
            }
        }
        return items;
    }

    @Override
    public int componentSlot(String address) {
        var comps = componentEnvironments();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] != null && comps[i].node() != null && address.equals(comps[i].node().address())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public double xPosition() {
        var p = player();
        return p != null ? p.getX() : 0;
    }

    @Override
    public double yPosition() {
        var p = player();
        return p != null ? p.getY() + p.getEyeHeight() : 0;
    }

    @Override
    public double zPosition() {
        var p = player();
        return p != null ? p.getZ() : 0;
    }

    @Override
    public Level level() {
        var p = player();
        return p != null ? p.level() : null;
    }

    @Override
    public Direction facing() {
        var p = player();
        return p != null ? RotationHelper.fromYaw(p.getYRot()) : Direction.NORTH;
    }

    @Override
    public Direction toLocal(Direction value) {
        return RotationHelper.toLocal(Direction.NORTH, facing(), value);
    }

    @Override
    public Direction toGlobal(Direction value) {
        return RotationHelper.toGlobal(Direction.NORTH, facing(), value);
    }

}
