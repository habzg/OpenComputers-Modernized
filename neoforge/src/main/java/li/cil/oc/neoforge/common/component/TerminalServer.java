package li.cil.oc.neoforge.common.component;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerminalServer implements Environment, EnvironmentHost, Analyzable, RackMountable, Lifecycle, DeviceInfo {
    public final Rack rack;
    public final int slot;
    public final Node node = Network.newNode(this, Visibility.None).create();
    private volatile TextBuffer buffer;
    private volatile Keyboard keyboard;

    public final double range = Settings.get().maxWirelessRange[Tier.Two];
    public final List<String> keys = new ArrayList<>();

    private final Map<String, String> deviceInfo;

    public TerminalServer(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;

        this.deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Terminal server",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "RemoteViewing EX"
        );
    }

    private synchronized TextBuffer createBuffer() {
        if (buffer != null) return buffer;
        var screenItem = Items.get(Constants.BlockName.ScreenTier1).createItemStack(1);
        var driver = Driver.driverFor(screenItem, getClass());
        if (driver == null) {
            throw new IllegalStateException("No driver registered for screen item; cannot initialise terminal server buffer.");
        }
        TextBuffer created = (TextBuffer) driver.createEnvironment(screenItem, this);
        if (created == null) {
            throw new IllegalStateException("Screen driver returned null environment; cannot initialise terminal server buffer.");
        }
        created.setMaximumResolution(Settings.screenResolutionsByTier[Tier.Three][0], Settings.screenResolutionsByTier[Tier.Three][1]);
        created.setMaximumColorDepth(Settings.screenDepthsByTier[Tier.Three]);
        buffer = created;
        return buffer;
    }

    private synchronized Keyboard createKeyboard() {
        if (keyboard != null) return keyboard;
        var keyboardItem = Items.get(Constants.BlockName.Keyboard).createItemStack(1);
        var keyboardDriver = Driver.driverFor(keyboardItem, getClass());
        if (keyboardDriver == null) {
            throw new IllegalStateException("No driver registered for keyboard item; cannot initialise terminal server keyboard.");
        }
        Keyboard created = (Keyboard) keyboardDriver.createEnvironment(keyboardItem, this);
        if (created == null) {
            throw new IllegalStateException("Keyboard driver returned null environment; cannot initialise terminal server keyboard.");
        }
        final TerminalServer self = this;
        created.setUsableOverride((kb, player) -> {
            var stack = player.getMainHandItem();
            if (stack.isEmpty()) return false;
            if (Items.get(stack) != Items.get(Constants.ItemName.Terminal)) return false;
            var cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd == null || cd.isEmpty()) return false;
            var tag = cd.copyTag();
            var key = tag.getString(Settings.namespace + "key");
            if (key.isEmpty()) return false;
            return self.sidedKeys().contains(key);
        });
        keyboard = created;
        return keyboard;
    }

    public TextBuffer buffer() {
        return createBuffer();
    }

    public TextBuffer bufferIfLoaded() {
        return buffer;
    }

    public Keyboard keyboard() {
        return createKeyboard();
    }

    public boolean hasAddress() {
        if (rack == null) return false;
        var data = rack.getMountableData(slot);
        if (data == null) return false;
        return data.contains("terminalAddress");
    }

    public String address() {
        if (rack == null) return null;
        var data = rack.getMountableData(slot);
        if (data == null) return null;
        var addr = data.getString("terminalAddress");
        return addr.isEmpty() ? null : addr;
    }

    public List<String> sidedKeys() {
        if (rack == null) return List.of();
        var level = rack.level();
        if (level == null || !level.isClientSide()) return keys;
        var data = rack.getMountableData(slot);
        if (data == null) return List.of();
        var tagList = data.getList("keys", Tag.TAG_STRING);
        var result = new ArrayList<String>(tagList.size());
        for (int i = 0; i < tagList.size(); i++) {
            result.add(tagList.getString(i));
        }
        return result;
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
    public void onConnect(Node node) {
        if (this.node == null) return;
        if (node != this.node) return;
        var buf = buffer();
        var kb = keyboard();
        var bufNode = buf.node();
        var kbNode = kb.node();
        if (bufNode != null) this.node.connect(bufNode);
        if (kbNode != null) this.node.connect(kbNode);
        if (bufNode != null && kbNode != null) bufNode.connect(kbNode);
    }

    @Override
    public void onDisconnect(Node node) {
        if (this.node == null) return;
        if (node != this.node) return;
        var buf = this.buffer;
        var kb = this.keyboard;
        if (buf != null && buf.node() != null) buf.node().remove();
        if (kb != null && kb.node() != null) kb.node().remove();
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public double xPosition() {
        return rack.xPosition();
    }

    @Override
    public double yPosition() {
        return rack.yPosition();
    }

    @Override
    public double zPosition() {
        return rack.zPosition();
    }

    @Override
    public Level level() {
        return rack.level();
    }

    @Override
    public void markChanged() {
        rack.markChanged();
    }

    @Override
    public CompoundTag getData() {
        var nbt = new CompoundTag();
        var keysList = new ListTag();
        for (var key : keys) {
            keysList.add(StringTag.valueOf(key));
        }
        nbt.put("keys", keysList);
        if (node != null) {
            if (node.address() == null) {
                Network.joinNewNetwork(node);
            }
            nbt.putString("terminalAddress", node.address() == null ? "" : node.address());
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
        if (heldItem.isEmpty()) return false;
        if (Items.get(heldItem) != Items.get(Constants.ItemName.Terminal)) return false;

        if (!level().isClientSide) {
            var key = UUID.randomUUID().toString();
            var cd = heldItem.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag;
            if (cd == null || cd.isEmpty()) {
                tag = new CompoundTag();
            } else {
                tag = cd.copyTag();
                var oldKey = tag.getString(Settings.namespace + "key");
                if (!oldKey.isEmpty()) {
                    keys.remove(oldKey);
                }
            }
            var maxSize = Settings.get().terminalsPerServer;
            while (keys.size() >= maxSize) {
                keys.removeFirst();
            }
            keys.add(key);
            tag.putString(Settings.namespace + "key", key);
            tag.putString(Settings.namespace + "server", node != null ? node.address() : "");
            heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            rack.markChanged(slot);
            PacketSender.sendRackMountableData((BlockEntity) rack, slot, getData());
            player.getInventory().setChanged();
        }
        return true;
    }

    private static final String BufferTag = Settings.namespace + "buffer";
    private static final String KeyboardTag = Settings.namespace + "keyboard";
    private static final String KeysTag = Settings.namespace + "keys";

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        var level = rack.level();
        if (level != null && !level.isClientSide() && node != null) {
            node.load(nbt, provider);
        }
        if (nbt.contains(BufferTag)) {
            buffer().load(nbt.getCompound(BufferTag), provider);
        }
        if (nbt.contains(KeyboardTag)) {
            keyboard().load(nbt.getCompound(KeyboardTag), provider);
        }
        keys.clear();
        var keysList = nbt.getList(KeysTag, Tag.TAG_STRING);
        for (int i = 0; i < keysList.size(); i++) {
            keys.add(keysList.getString(i));
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        if (node != null) {
            node.save(nbt, provider);
        }
        if (buffer != null) {
            var bufferNbt = new CompoundTag();
            buffer.save(bufferNbt, provider);
            nbt.put(BufferTag, bufferNbt);
        }
        if (keyboard != null) {
            var keyboardNbt = new CompoundTag();
            keyboard.save(keyboardNbt, provider);
            nbt.put(KeyboardTag, keyboardNbt);
        }
        var keysList = new ListTag();
        for (var key : keys) {
            keysList.add(StringTag.valueOf(key));
        }
        nbt.put(KeysTag, keysList);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        var level = rack.level();
        var isClient = level != null && level.isClientSide();
        if (isClient || (node != null && node.address() != null && node.network() != null)) {
            buffer().update();
        }
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.noneOf(StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(Player player, int side, float hitX, float hitY, float hitZ) {
        var buf = buffer();
        var kb = keyboard();
        return new Node[]{
                buf != null ? buf.node() : null,
                kb != null ? kb.node() : null
        };
    }

    @Override
    public void onLifecycleStateChange(Lifecycle.LifecycleState state) {
        var level = rack.level();
        if (level == null || !level.isClientSide()) return;
        if (state == Lifecycle.LifecycleState.Initialized) {
            TerminalServerCache.loaded.add(this);
        } else if (state == Lifecycle.LifecycleState.Disposed) {
            TerminalServerCache.loaded.remove(this);
        }
    }

    public static class TerminalServerCache {
        private final Map<String, TerminalServer> ready = new HashMap<>();
        private final List<TerminalServer> pending = new ArrayList<>();
        public static final TerminalServerCache loaded = new TerminalServerCache();

        public void completePending() {
            var promoted = new ArrayList<TerminalServer>();
            for (var term : pending) {
                if (term.hasAddress()) {
                    promoted.add(term);
                }
            }
            for (var term : promoted) {
                pending.remove(term);
                var address = term.address();
                if (address == null) continue;
                ready.put(address, term);
            }
        }

        @SuppressWarnings({"UnusedReturnValue", "SameReturnValue"})
        public boolean add(TerminalServer terminal) {
            synchronized (this) {
                completePending();
                if (terminal.hasAddress()) {
                    var newAddress = terminal.address();
                    if (newAddress == null) {
                        pending.add(terminal);
                        return true;
                    }
                    ready.put(newAddress, terminal);
                    return true;
                } else {
                    pending.add(terminal);
                    return true;
                }
            }
        }

        @SuppressWarnings("UnusedReturnValue")
        public boolean remove(TerminalServer terminal) {
            synchronized (this) {
                completePending();
                if (terminal.hasAddress()) {
                    var addr = terminal.address();
                    if (addr == null) {
                        return pending.remove(terminal);
                    }
                    var existing = ready.get(addr);
                    if (existing == terminal) {
                        ready.remove(addr);
                        return true;
                    }
                    return pending.remove(terminal);
                } else {
                    return pending.remove(terminal);
                }
            }
        }

        public void clear() {
            synchronized (this) {
                ready.clear();
                pending.clear();
            }
        }

        public TerminalServer find(String address) {
            synchronized (this) {
                completePending();
                return ready.get(address);
            }
        }
    }
}
