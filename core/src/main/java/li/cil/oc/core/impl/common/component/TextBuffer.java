package li.cil.oc.core.impl.common.component;

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientComponentTracker;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.Screen;
import li.cil.oc.core.impl.server.component.Keyboard;
import li.cil.oc.core.impl.util.SaveHandlerDelegate;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.util.ClientPacketSenderDelegate;
import li.cil.oc.core.util.PacketBuilderFactory;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class TextBuffer extends TextBufferBase {
  public static final List<TextBuffer> clientBuffers = new ArrayList<>();

    public TextBuffer(EnvironmentHost host) {
        super(host);
    }

    @Override
    protected Proxy createProxy() {
        if (SideTracker.isClient()) {
            return new ClientProxy(this);
        } else {
            return new ServerProxy(this);
        }
    }

    @Override
    public int renderWidth() {
        return TextBufferRenderCache.renderer.charRenderWidth() * viewportW;
    }

    @Override
    public int renderHeight() {
        return TextBufferRenderCache.renderer.charRenderHeight() * viewportH;
    }

    public static void registerClientBuffer(TextBuffer t) {
      ClientPacketSenderDelegate.get().sendTextBufferInit(t.proxy.nodeAddress);
        ClientComponentTracker.INSTANCE.add(t.host().level(), t.proxy.nodeAddress, t);
        clientBuffers.add(t);
    }

    private PacketBuilderBase<?> pendingCommands = null;

    private PacketBuilderBase<?> pendingCommands() {
        if (pendingCommands == null) {
            pendingCommands = (PacketBuilderBase<?>) PacketBuilderFactory.get().createCompressed(PacketType.TextBufferMulti);
            try {
                pendingCommands.writeUTF(node.address());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return pendingCommands;
    }

    @Override
    protected void sendPowerChange(String address, boolean powered) {
                    PacketSender.sendTextBufferPowerChange(address, powered, host());
    }

    @Override
    protected void flushPendingCommands() {
        boolean hadCommands;
        synchronized (this) {
            hadCommands = pendingCommands != null;
            if (hadCommands) {
                pendingCommands.sendToPlayersNearHost(host(), OCSettings.get().maxWirelessRange[Tier.Two] * OCSettings.get().maxWirelessRange[Tier.Two]);
                pendingCommands = null;
            }
        }
        if (hadCommands) {
            host().markChanged();
        }
    }

    @Override
    protected void sendClientBufferInit(String nodeAddress) {
                    ClientPacketSenderDelegate.get().sendTextBufferInit(nodeAddress);
    }

    @Override
    protected Object[] getKeyboardsImpl(Context computer, Arguments args) {
        computer.pause(0.25);
        if (host() instanceof Screen screen) {
            List<String> addrs = new ArrayList<>();
            for (var s : screen.screens) {
                for (var n : s.node().neighbors()) {
                    if (n.host() instanceof Keyboard) addrs.add(n.address());
                }
            }
            return ResultWrapper.result((Object) addrs.toArray(new String[0]));
        } else {
            List<String> addrs = new ArrayList<>();
            for (var n : node.neighbors()) {
                if (n.host() instanceof Keyboard) addrs.add(n.address());
            }
            return ResultWrapper.result((Object) addrs.toArray(new String[0]));
        }
    }

    @Override
    protected void onClientLoad(CompoundTag nbt, HolderLookup.Provider provider) {
        if (!Strings.isNullOrEmpty(proxy.nodeAddress)) return;
        proxy.nodeAddress = nbt.getCompound("node").getString("address");
        if (!Strings.isNullOrEmpty(proxy.nodeAddress)) {
            registerClientBuffer(this);
        }
    }

    @Override
    protected void loadBufferData(CompoundTag nbt, HolderLookup.Provider provider) {
        data.load(SaveHandlerDelegate.loadNBTFrom(nbt, node.address() + "_buffer"), provider);
    }

    @Override
    protected void scheduleBufferSave(CompoundTag nbt, String key, byte[] data) {
        SaveHandlerDelegate.schedule(host(), nbt, key, data);
    }

    @Override
    protected void registerComponent(String address) {
        var level = host().level();
        if (level != null) {
            li.cil.oc.core.impl.common.ComponentTracker.getServerTracker().add(level, address, this);
        }
    }

    @Override
    protected void unregisterComponent() {
        var level = host().level();
        if (level != null) {
            li.cil.oc.core.impl.common.ComponentTracker.getServerTracker().remove(level, this);
            if (SideTracker.isClient()) {
                ClientComponentTracker.INSTANCE.remove(level, this);
            }
        }
    }

    public static class ClientProxy extends Proxy {
        public ClientProxy(TextBuffer owner) {
            super(owner);
        }

        @Override
        public void onBufferColorChange() {
            markDirty();
        }

        @Override
        public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferDepthChange(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
            markDirty();
        }

        @Override
        public void onBufferFill(int col, int row, int w, int h, int c) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferPaletteChange(int index) {
            markDirty();
        }

        @Override
        public void onBufferResolutionChange(int w, int h) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferViewportResolutionChange(int w, int h) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferSet(int col, int row, String s, boolean vertical) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferRamInit(GpuTextBuffer ram) {
            owner.relativeLitArea = -1;
        }

        @Override
        public void onBufferRamDestroy(GpuTextBuffer ram) {
            owner.relativeLitArea = -1;
        }

        @Override
        public void onBufferRawSetText(int col, int row, int[][] text) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferRawSetBackground(int col, int row, int[][] color) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void onBufferRawSetForeground(int col, int row, int[][] color) {
            owner.relativeLitArea = -1;
            markDirty();
        }

        @Override
        public void keyDown(char character, int code, Player player) {
            if (!nodeAddress.isEmpty()) {
                                    ClientPacketSenderDelegate.get().sendKeyDown(nodeAddress, character, code);
            }
        }

        @Override
        public void keyUp(char character, int code, Player player) {
            if (!nodeAddress.isEmpty()) {
                                    ClientPacketSenderDelegate.get().sendKeyUp(nodeAddress, character, code);
            }
        }

        @Override
        public void clipboard(String value, Player player) {
            if (!nodeAddress.isEmpty()) {
                ClientPacketSenderDelegate.get().sendClipboard(nodeAddress, value);
            }
        }

        @Override
        public void dropFile(String fileName, String fileContent, Player player) {
            if (!nodeAddress.isEmpty()) {
                ClientPacketSenderDelegate.get().sendDropFile(nodeAddress, fileName, fileContent);
            }
        }

        @Override
        public void mouseDown(double x, double y, int button, Player player) {
            if (!nodeAddress.isEmpty()) {
                                    ClientPacketSenderDelegate.get().sendMouseClick(nodeAddress, x, y, false, button);
            }
        }

        @Override
        public void mouseDrag(double x, double y, int button, Player player) {
            if (!nodeAddress.isEmpty()) {
                                    ClientPacketSenderDelegate.get().sendMouseClick(nodeAddress, x, y, true, button);
            }
        }

        @Override
        public void mouseUp(double x, double y, int button, Player player) {
            if (!nodeAddress.isEmpty()) {
                                    ClientPacketSenderDelegate.get().sendMouseUp(nodeAddress, x, y, button);
            }
        }

        @Override
        public void mouseScroll(double x, double y, int delta, Player player) {
            if (!nodeAddress.isEmpty()) {
                                    ClientPacketSenderDelegate.get().sendMouseScroll(nodeAddress, x, y, delta);
            }
        }

        @Override
        public void copyToAnalyzer(int line, Player player) {
            if (!nodeAddress.isEmpty()) {
                ClientPacketSenderDelegate.get().sendCopyToAnalyzer(nodeAddress, line);
            }
        }
    }

    public static class ServerProxy extends Proxy {
        public ServerProxy(TextBuffer owner) {
            super(owner);
        }

        @Override
        public void onBufferColorChange() {

            synchronized (owner) {
                                    PacketSender.appendTextBufferColorChange(((TextBuffer) owner).pendingCommands(), owner.data.foreground(), owner.data.background());
            }
        }

        @Override
        public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferCopy(((TextBuffer) owner).pendingCommands(), col, row, w, h, tx, ty);
            }
        }

        @Override
        public void onBufferDepthChange(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {

            synchronized (owner) {
                                    PacketSender.appendTextBufferDepthChange(((TextBuffer) owner).pendingCommands(), depth);
            }
        }

        @Override
        public void onBufferFill(int col, int row, int w, int h, int c) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferFill(((TextBuffer) owner).pendingCommands(), col, row, w, h, c);
            }
        }

        @Override
        public void onBufferPaletteChange(int index) {

            synchronized (owner) {
                                    PacketSender.appendTextBufferPaletteChange(((TextBuffer) owner).pendingCommands(), index, owner.getPaletteColor(index));
            }
        }

        @Override
        public void onBufferResolutionChange(int w, int h) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferResolutionChange(((TextBuffer) owner).pendingCommands(), w, h);
            }
        }

        @Override
        public void onBufferViewportResolutionChange(int w, int h) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferViewportResolutionChange(((TextBuffer) owner).pendingCommands(), w, h);
            }
        }

        @Override
        public void onBufferMaxResolutionChange(int w, int h) {
            if (owner.node.network() != null) {
                owner.relativeLitArea = -1;

                synchronized (owner) {
                                            PacketSender.appendTextBufferMaxResolutionChange(((TextBuffer) owner).pendingCommands(), w, h);
                }
            }
        }

        @Override
        public void onBufferSet(int col, int row, String s, boolean vertical) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferSet(((TextBuffer) owner).pendingCommands(), col, row, s, vertical);
            }
        }

        @Override
        public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferBitBlt(((TextBuffer) owner).pendingCommands(), col, row, w, h, ram.owner, ram.id, fromCol, fromRow);
            }
        }

        @Override
        public void onBufferRamInit(GpuTextBuffer ram) {
            owner.relativeLitArea = -1;

            var nbt = new CompoundTag();
            ram.save(nbt, null);
            synchronized (owner) {
                                    PacketSender.appendTextBufferRamInit(((TextBuffer) owner).pendingCommands(), ram.owner, ram.id, nbt);
            }
        }

        @Override
        public void onBufferRamDestroy(GpuTextBuffer ram) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferRamDestroy(((TextBuffer) owner).pendingCommands(), ram.owner, ram.id);
            }
        }

        @Override
        public void onBufferRawSetText(int col, int row, int[][] text) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferRawSetText(((TextBuffer) owner).pendingCommands(), col, row, text);
            }
        }

        @Override
        public void onBufferRawSetBackground(int col, int row, int[][] color) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferRawSetBackground(((TextBuffer) owner).pendingCommands(), col, row, color);
            }
        }

        @Override
        public void onBufferRawSetForeground(int col, int row, int[][] color) {
            owner.relativeLitArea = -1;

            synchronized (owner) {
                                    PacketSender.appendTextBufferRawSetForeground(((TextBuffer) owner).pendingCommands(), col, row, color);
            }
        }

        @Override
        public void keyDown(char character, int code, Player player) {
            sendToKeyboards("keyboard.keyDown", player, character, code);
        }

        @Override
        public void keyUp(char character, int code, Player player) {
            sendToKeyboards("keyboard.keyUp", player, character, code);
        }

        @Override
        public void clipboard(String value, Player player) {
            sendToKeyboards("keyboard.clipboard", player, value);
        }

        @Override
        public void dropFile(String fileName, String fileContent, Player player) {
                            owner.node.sendToReachable("computer.checked_signal", player, "drop_file", fileName, fileContent);
        }

        @Override
        public void mouseDown(double x, double y, int button, Player player) {
            sendMouseEvent(player, "touch", x, y, button);
        }

        @Override
        public void mouseDrag(double x, double y, int button, Player player) {
            sendMouseEvent(player, "drag", x, y, button);
        }

        @Override
        public void mouseUp(double x, double y, int button, Player player) {
            sendMouseEvent(player, "drop", x, y, button);
        }

        @Override
        public void mouseScroll(double x, double y, int delta, Player player) {
            sendMouseEvent(player, "scroll", x, y, delta);
        }

        @Override
        public void copyToAnalyzer(int line, Player player) {
            if (player == null) return;
            var stack = player.getMainHandItem();
            if (stack.isEmpty()) return;
            var existing = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            var tag = existing != null ? existing.copyTag() : new CompoundTag();
            tag.remove(OCSettings.namespace + "clipboard");
            if (line >= 0 && line < owner.getViewportHeight()) {
                var text = owner.data.lineToString(line);
                if (!Strings.isNullOrEmpty(text)) {
                    tag.putString(OCSettings.namespace + "clipboard", text);
                }
            }
            if (tag.isEmpty()) {
                stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            } else {
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(tag));
            }
        }

        private void sendMouseEvent(Player player, String name, double x, double y, int data) {
            var argsList = new ArrayList<>();
            argsList.add(player);
            argsList.add(name);
            if (owner.precisionMode) {
                argsList.add(x);
                argsList.add(y);
            } else {
                argsList.add((int) x + 1);
                argsList.add((int) y + 1);
            }
            argsList.add(data);
            if (OCSettings.get().inputUsername && player != null) {
                argsList.add(player.getScoreboardName());
            }
            owner.node.sendToReachable("computer.checked_signal", argsList.toArray());
        }

        private void sendToKeyboards(String name, Object... values) {
            if (owner.host() instanceof Screen screen) {
                for (var s : screen.screens) {
                    s.node().sendToNeighbors(name, values);
                }
            } else {
                owner.node.sendToNeighbors(name, values);
            }
        }
    }
}
