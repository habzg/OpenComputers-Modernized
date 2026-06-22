package li.cil.oc.core.impl.common.component;

import com.google.common.base.Strings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.component.traits.TextBufferProxy;
import li.cil.oc.core.impl.common.component.traits.VideoRamDevice;
import li.cil.oc.core.impl.common.component.traits.VideoRamRasterizer;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public abstract class TextBufferBase extends ManagedEnvironment implements TextBufferProxy, VideoRamRasterizer, DeviceInfo {
    public final Node node;
    private final int syncInterval = 100;
    private final Map<String, String> deviceInfo = new HashMap<>();

    public double fullyLitCost;
    public Proxy proxy;
    public int viewportW, viewportH;
    private int maxWidth;
    private int maxHeight;
    private int maxDepth = Settings.screenDepthsByTier[Tier.One].ordinal();
    private final int[] initialRes = Settings.screenResolutionsByTier[Tier.One];
    public final li.cil.oc.core.impl.util.TextBuffer data = new li.cil.oc.core.impl.util.TextBuffer(
            new int[]{initialRes[0], initialRes[1]}, PackedColor.Depth.format(li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[maxDepth]));
    private double aspectRatioW = 1.0;
    private double aspectRatioH = 1.0;
    private double powerConsumptionPerTick = Settings.get().screenCost;
    protected boolean precisionMode = false;
    private boolean isRendering = true;
    private boolean isDisplaying = true;
    private boolean hasPower = true;
    protected double relativeLitArea = -1.0;
    private int syncCooldown = syncInterval;

    private final Map<String, VideoRamDevice> internalBuffers = new HashMap<>();

    {
        deviceInfo.put(DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display);
        deviceInfo.put(DeviceInfo.DeviceAttribute.Description, "Text buffer");
        deviceInfo.put(DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        deviceInfo.put(DeviceInfo.DeviceAttribute.Product, "Text Screen V0");
        deviceInfo.put(DeviceInfo.DeviceAttribute.Width, String.valueOf(new int[]{1, 4, 8}[maxDepth]));
    }

    protected TextBufferBase(EnvironmentHost host) {
        super(host);
        this.node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent("screen")
                .withConnector()
                .create();
        var res = Settings.screenResolutionsByTier[Tier.One];
        maxWidth = res[0];
        maxHeight = res[1];
        viewportW = res[0];
        viewportH = res[1];
        fullyLitCost = computeFullyLitCost();
        proxy = createProxy();
    }


    protected abstract Proxy createProxy();

    public double computeFullyLitCost() {
        var res = Settings.screenResolutionsByTier[0];
        int w = res[0], h = res[1];
        int mw = getMaximumWidth();
        int mh = getMaximumHeight();
        return powerConsumptionPerTick * (mw * mh) / (double) (w * h);
    }

    @Override
    public Map<String, VideoRamDevice> getInternalBuffers() {
        return internalBuffers;
    }

    public void markInitialized() {
        syncCooldown = -1;
        relativeLitArea = -1;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    protected abstract void sendPowerChange(String address, boolean powered);

    protected abstract void flushPendingCommands() ;

    protected abstract void sendClientBufferInit(String nodeAddress);

    @Override
    public void update() {
        super.update();
        if (isDisplaying && host().level().getGameTime() % Settings.get().tickFrequency == 0) {
            if (relativeLitArea < 0) {
                int w = getViewportWidth();
                int h = getViewportHeight();
                float acc = 0;
                for (int y = 0; y < h; y++) {
                    var line = data.buffer[y];
                    var colors = data.color[y];
                    for (int x = 0; x < w; x++) {
                        int ch = line[x];
                        int col = colors[x];
                        int bg = PackedColor.unpackBackground((short) col, data.format());
                        int fg = PackedColor.unpackForeground((short) col, data.format());
                        if (ch == ' ') acc += (bg == 0 ? 0 : 1);
                        else if (ch == 0x2588) acc += (fg == 0 ? 0 : 1);
                        else acc += (fg == 0 && bg == 0 ? 0 : 1);
                    }
                }
                relativeLitArea = acc / (double) (w * h);
            }
            if (node != null) {
                boolean hadPower = hasPower;
                double neededPower = relativeLitArea * fullyLitCost * Settings.get().tickFrequency;
                hasPower = ((Connector) node).tryChangeBuffer(-neededPower);
                if (hasPower != hadPower) {
                    sendPowerChange(node.address(), isDisplaying && hasPower);
                }
            }
        }
        flushPendingCommands();
        if (SideTracker.isClient() && syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0) {
                syncCooldown = syncInterval;
                sendClientBufferInit(proxy.nodeAddress);
            }
        }
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is currently on.")
    public Object[] isOn(Context computer, Arguments args) {
        return ResultWrapper.result(isDisplaying);
    }

    @Callback(doc = "function():boolean -- Turns the screen on. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOn(Context computer, Arguments args) {
        boolean old = isDisplaying;
        setPowerState(true);
        return ResultWrapper.result(isDisplaying != old, isDisplaying);
    }

    @Callback(doc = "function():boolean -- Turns the screen off. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOff(Context computer, Arguments args) {
        boolean old = isDisplaying;
        setPowerState(false);
        return ResultWrapper.result(isDisplaying != old, isDisplaying);
    }

    @Callback(direct = true, doc = "function():number, number -- The aspect ratio of the screen. For multi-block screens this is the number of blocks, horizontal and vertical.")
    public Object[] getAspectRatio(Context computer, Arguments args) {
        synchronized (this) {
            return ResultWrapper.result(aspectRatioW, aspectRatioH);
        }
    }

    @SuppressWarnings("unused")
    protected abstract Object[] getKeyboardsImpl(Context computer, Arguments args);

    @Callback(doc = "function():table -- The list of keyboards attached to the screen.")
    public Object[] getKeyboards(Context computer, Arguments args) {
        return getKeyboardsImpl(computer, args);
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is in high precision mode (sub-pixel mouse event positions).")
    public Object[] isPrecise(Context computer, Arguments args) {
        return ResultWrapper.result(precisionMode);
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Set whether to use high precision mode (sub-pixel mouse event positions).")
    public Object[] setPrecise(Context computer, Arguments args) {
        if (maxDepth == Settings.screenDepthsByTier[3].ordinal()) {
            boolean old = precisionMode;
            precisionMode = args.checkBoolean(0);
            return ResultWrapper.result(old);
        }
        return ResultWrapper.result(null, "unsupported operation");
    }

    @Override
    public double getEnergyCostPerTick() {
        return powerConsumptionPerTick;
    }

    @Override
    public void setEnergyCostPerTick(double value) {
        powerConsumptionPerTick = value;
        fullyLitCost = computeFullyLitCost();
    }

    @Override
    public boolean getPowerState() {
        return isDisplaying;
    }

    @Override
    public void setPowerState(boolean value) {
        if (isDisplaying != value) {
            isDisplaying = value;
            if (isDisplaying) {
                double needed = fullyLitCost * Settings.get().tickFrequency;
                hasPower = ((Connector) node).changeBuffer(-needed) == 0;
            }
            sendPowerChange(node.address(), isDisplaying && hasPower);
        }
    }

    @Override
    public void setMaximumResolution(int width, int height) {
        if (width < 1) throw new IllegalArgumentException("width must be >= 1");
        if (height < 1) throw new IllegalArgumentException("height must be >= 1");
        maxWidth = width;
        maxHeight = height;
        fullyLitCost = computeFullyLitCost();
        proxy.onBufferMaxResolutionChange(width, height);
    }

    @Override
    public int getMaximumWidth() {
        return maxWidth;
    }

    @Override
    public int getMaximumHeight() {
        return maxHeight;
    }

    @Override
    public void setAspectRatio(double width, double height) {
        synchronized (this) {
            aspectRatioW = width;
            aspectRatioH = height;
        }
    }

    @Override
    public double getAspectRatio() {
        return aspectRatioW / aspectRatioH;
    }

    @Override
    public boolean setResolution(int w, int h) {
        if (w < 1 || h < 1 || w > maxWidth || h > maxHeight || h * w > maxWidth * maxHeight)
            throw new IllegalArgumentException("unsupported resolution");
        proxy.onBufferResolutionChange(w, h);
        int oldWidth = data.width, oldHeight = data.height;
        data.size_$eq(new int[]{w, h});
        boolean sizeChanged = data.width != oldWidth || data.height != oldHeight;
        boolean viewportChanged = setViewport(w, h);
        if (sizeChanged || viewportChanged) {
            if (!viewportChanged && node != null)
                node.sendToReachable("computer.signal", "screen_resized", w, h);
            return true;
        }
        return false;
    }

    @Override
    public boolean setViewport(int w, int h) {
        if (w < 1 || h < 1 || w > data.width || h > data.height)
            throw new IllegalArgumentException("unsupported viewport resolution");
        proxy.onBufferViewportResolutionChange(w, h);
        if (w != viewportW || h != viewportH) {
            viewportW = w;
            viewportH = h;
            if (node != null)
                node.sendToReachable("computer.signal", "screen_resized", w, h);
            return true;
        }
        return false;
    }

    @Override
    public int getViewportWidth() {
        return viewportW;
    }

    @Override
    public int getViewportHeight() {
        return viewportH;
    }

    @Override
    public li.cil.oc.api.internal.TextBuffer.ColorDepth getMaximumColorDepth() {
        return li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[maxDepth];
    }

    @Override
    public void setMaximumColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        maxDepth = depth.ordinal();
    }

    @Override
    public boolean setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        boolean changed = TextBufferProxy.super.setColorDepth(depth);
        proxy.onBufferDepthChange(depth);
        return changed;
    }

    @Override
    public void onBufferPaletteChange(int index) {
        proxy.onBufferPaletteChange(index);
    }

    @Override
    public void onBufferColorChange() {
        proxy.onBufferColorChange();
    }

    @Override
    public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
        proxy.onBufferCopy(col, row, w, h, tx, ty);
    }

    @Override
    public void onBufferFill(int col, int row, int w, int h, int c) {
        proxy.onBufferFill(col, row, w, h, c);
    }

    @Override
    public void onBufferSet(int col, int row, String s, boolean vertical) {
        proxy.onBufferSet(col, row, s, vertical);
    }

    @Override
    public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
        proxy.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow);
    }

    @Override
    public void onBufferRamInit(GpuTextBuffer ram) {
        proxy.onBufferRamInit(ram);
    }

    @Override
    public void onBufferRamDestroy(GpuTextBuffer ram) {
        proxy.onBufferRamDestroy(ram);
    }

    @Override
    public void rawSetText(int col, int row, int[][] text) {
        TextBufferProxy.super.rawSetText(col, row, text);
        proxy.onBufferRawSetText(col, row, text);
    }

    @Override
    public void rawSetBackground(int col, int row, int[][] color) {
        TextBufferProxy.super.rawSetBackground(col, row, color);
        proxy.onBufferRawSetBackground(col, row, color);
    }

    @Override
    public void rawSetForeground(int col, int row, int[][] color) {
        TextBufferProxy.super.rawSetForeground(col, row, color);
        proxy.onBufferRawSetForeground(col, row, color);
    }

    public boolean hasLitContent() {
        return relativeLitArea != 0;
    }
    public boolean isBufferDirty() {
        return proxy.dirty;
    }
    public void clearBufferDirty() {
        proxy.dirty = false;
    }

    @Override
    public int renderWidth() {
        return 0;
    }

    @Override
    public int renderHeight() {
        return 0;
    }

    @Override
    public boolean isRenderingEnabled() {
        return isRendering;
    }

    @Override
    public void setRenderingEnabled(boolean enabled) {
        isRendering = enabled;
    }

    @Override
    public void keyDown(char character, int code, Player player) {
        proxy.keyDown(character, code, player);
    }

    @Override
    public void keyUp(char character, int code, Player player) {
        proxy.keyUp(character, code, player);
    }

    @Override
    public void clipboard(String value, Player player) {
        proxy.clipboard(value, player);
    }

    @Override
    public void dropFile(String fileName, String fileContent, Player player) {
        proxy.dropFile(fileName, fileContent, player);
    }

    @Override
    public void mouseDown(double x, double y, int button, Player player) {
        proxy.mouseDown(x, y, button, player);
    }

    @Override
    public void mouseDrag(double x, double y, int button, Player player) {
        proxy.mouseDrag(x, y, button, player);
    }

    @Override
    public void mouseUp(double x, double y, int button, Player player) {
        proxy.mouseUp(x, y, button, player);
    }

    @Override
    public void mouseScroll(double x, double y, int delta, Player player) {
        proxy.mouseScroll(x, y, delta, player);
    }

    public void copyToAnalyzer(int line, Player player) {
        proxy.copyToAnalyzer(line, player);
    }

    protected abstract void registerComponent(String address);

    protected abstract void unregisterComponent();

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            registerComponent(node.address());
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            unregisterComponent();
        }
    }

    @SuppressWarnings("unused")
    protected abstract void onClientLoad(CompoundTag nbt, HolderLookup.Provider provider) ;

    protected abstract void loadBufferData(CompoundTag nbt, HolderLookup.Provider provider);

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (SideTracker.isClient()) {
            onClientLoad(nbt, provider);
        } else {
            if (nbt.contains("buffer")) data.load(nbt.getCompound("buffer"), provider);
            else if (!Strings.isNullOrEmpty(node.address()))
                loadBufferData(nbt, provider);
        }
        if (nbt.contains(Settings.namespace + "isOn"))
            isDisplaying = nbt.getBoolean(Settings.namespace + "isOn");
        if (nbt.contains(Settings.namespace + "hasPower"))
            hasPower = nbt.getBoolean(Settings.namespace + "hasPower");
        if (nbt.contains(Settings.namespace + "maxWidth") && nbt.contains(Settings.namespace + "maxHeight")) {
            maxWidth = nbt.getInt(Settings.namespace + "maxWidth");
            maxHeight = nbt.getInt(Settings.namespace + "maxHeight");
        }
        precisionMode = nbt.getBoolean(Settings.namespace + "precise");
        if (nbt.contains(Settings.namespace + "viewportWidth")) {
            viewportW = Math.clamp(nbt.getInt(Settings.namespace + "viewportWidth"), 1, data.width);
            viewportH = Math.clamp(nbt.getInt(Settings.namespace + "viewportHeight"), 1, data.height);
        } else {
            viewportW = data.width;
            viewportH = data.height;
        }
    }

    protected abstract void scheduleBufferSave(CompoundTag nbt, String key, byte[] data);

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        if (node == null) return;
        super.save(nbt, provider);
        if (node.network() != null) {
            for (var n : node.network().nodes()) {
                if (n.host() instanceof li.cil.oc.core.impl.common.tileentity.traits.Computer comp && !comp.machine().isPaused()) {
                    comp.machine().pause(0.1);
                }
            }
        }
        try {
            var bufferTag = new CompoundTag();
            data.save(bufferTag, provider);
            var baos = new java.io.ByteArrayOutputStream();
            var dos = new java.io.DataOutputStream(baos);
            net.minecraft.nbt.NbtIo.write(bufferTag, dos);
            scheduleBufferSave(nbt, node.address() + "_buffer", baos.toByteArray());
        } catch (java.io.IOException e) {
            Log.get().warn("Failed to serialize text buffer data for saving.", e);
        }
        nbt.putBoolean(Settings.namespace + "isOn", isDisplaying);
        nbt.putBoolean(Settings.namespace + "hasPower", hasPower);
        nbt.putInt(Settings.namespace + "maxWidth", maxWidth);
        nbt.putInt(Settings.namespace + "maxHeight", maxHeight);
        nbt.putBoolean(Settings.namespace + "precise", precisionMode);
        nbt.putInt(Settings.namespace + "viewportWidth", viewportW);
        nbt.putInt(Settings.namespace + "viewportHeight", viewportH);
    }

    @Override
    public li.cil.oc.core.impl.util.TextBuffer data() {
        return data;
    }

    public abstract static class Proxy {
        public final TextBufferBase owner;
        public boolean dirty = false;
        public String nodeAddress = "";

        protected Proxy(TextBufferBase owner) {
            this.owner = owner;
        }

        public void markDirty() {
            dirty = true;
        }

        public abstract void onBufferColorChange();

        public abstract void onBufferCopy(int col, int row, int w, int h, int tx, int ty);

        public abstract void onBufferDepthChange(li.cil.oc.api.internal.TextBuffer.ColorDepth depth);

        public abstract void onBufferFill(int col, int row, int w, int h, int c);

        public abstract void onBufferPaletteChange(int index);

        public abstract void onBufferResolutionChange(int w, int h);

        public abstract void onBufferViewportResolutionChange(int w, int h);

        public void onBufferMaxResolutionChange(int w, int h) {
        }

        public abstract void onBufferSet(int col, int row, String s, boolean vertical);

        public abstract void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow);

        public abstract void onBufferRamInit(GpuTextBuffer ram);

        public abstract void onBufferRamDestroy(GpuTextBuffer ram);

        public abstract void onBufferRawSetText(int col, int row, int[][] text);

        public abstract void onBufferRawSetBackground(int col, int row, int[][] color);

        public abstract void onBufferRawSetForeground(int col, int row, int[][] color);

        public abstract void keyDown(char character, int code, Player player);

        public abstract void keyUp(char character, int code, Player player);

        public abstract void clipboard(String value, Player player);

        public abstract void dropFile(String fileName, String fileContent, Player player);

        public abstract void mouseDown(double x, double y, int button, Player player);

        public abstract void mouseDrag(double x, double y, int button, Player player);

        public abstract void mouseUp(double x, double y, int button, Player player);

        public abstract void mouseScroll(double x, double y, int delta, Player player);

        @SuppressWarnings({"unused", "EmptyMethod"})
        public abstract void copyToAnalyzer(int line, Player player);
    }
}
