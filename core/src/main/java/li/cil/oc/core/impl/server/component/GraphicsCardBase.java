package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.component.GpuTextBuffer;
import li.cil.oc.core.impl.common.component.traits.VideoRamRasterizer;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.util.ExtendedUnicodeHelper;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class GraphicsCardBase extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphicsCardBase.class);
    public final int tier;
    public final Connector node = Network.newNode(this, Visibility.Neighbors)
            .withComponent("gpu")
            .withConnector()
            .create();
    public final double[] setBackgroundCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128};
    public final double[] setForegroundCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128};
    public final double[] setPaletteColorCosts = {1.0 / 2, 1.0 / 8, 1.0 / 16};
    public final double[] setCosts = {1.0 / 64, 1.0 / 128, 1.0 / 256};
    public final double[] copyCosts = {1.0 / 16, 1.0 / 32, 1.0 / 64};
    public final double[] fillCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128};
    public final double bitbltCost;
    public final double totalVRAM;
    private final int[] maxResolution;
    private final TextBuffer.ColorDepth maxDepth;
    private final Map<Integer, GpuTextBuffer> buffers = new HashMap<>();
    private final Map<String, String> deviceInfo;
    public boolean budgetExhausted = false;
    private String screenAddress = null;
    private final Object screenLock = new Object();
    private TextBuffer screenInstance = null;
    private int bufferIndex = 0;
    private int nextBufferId = 1;

    public GraphicsCardBase(int tier) {
        this.tier = tier;
        this.maxResolution = Settings.screenResolutionsByTier[tier];
        this.maxDepth = Settings.screenDepthsByTier[tier];
        this.bitbltCost = Settings.get().bitbltCost * Math.pow(2, tier);
        this.totalVRAM = (maxResolution[0] * maxResolution[1]) * Settings.get().vramSizes[Math.clamp(tier, 0, 2)];

        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Display, DeviceAttribute.Description, "Graphics controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "MPG" + ((tier + 1) * 1000) + " GTZ", DeviceAttribute.Capacity, capacityInfo(), DeviceAttribute.Width, widthInfo(), DeviceAttribute.Clock, clockInfo());
        setNode(this.node);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    public String capacityInfo() {
        return String.valueOf(maxResolution[0] * maxResolution[1]);
    }

    public String widthInfo() {
        return new String[]{"1", "4", "8"}[maxDepth.ordinal()];
    }

    public String clockInfo() {
        return ((2000 / setBackgroundCosts[tier]) / 100) + "/" +
                ((2000 / setForegroundCosts[tier]) / 100) + "/" +
                ((2000 / setPaletteColorCosts[tier]) / 100) + "/" +
                ((2000 / setCosts[tier]) / 100) + "/" +
                ((2000 / copyCosts[tier]) / 100) + "/" +
                ((2000 / fillCosts[tier]) / 100);
    }

    private Object[] screen(int index, Function<TextBuffer, Object[]> f) {
        if (index == 0) {
            if (screenInstance != null) {
                synchronized (screenLock) {
                    return f.apply(screenInstance);
                }
            }
            return new Object[]{null, "no screen"};
        } else {
            TextBuffer buf = getBuffer(index);
            if (buf == null) {
                return new Object[]{null, "invalid buffer index"};
            }
            return f.apply(buf);
        }
    }

    private Object[] screen(Function<TextBuffer, Object[]> f) {
        return screen(bufferIndex, f);
    }

    private GpuTextBuffer getBuffer(int index) {
        return buffers.get(index);
    }

    private int[] bufferIndexes() {
        return buffers.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    private int calculateUsedMemory() {
        int used = 0;
        for (GpuTextBuffer buf : buffers.values()) {
            used += buf.data.width * buf.data.height;
        }
        return used;
    }

    private int nextAvailableBufferIndex() {
        int idx = nextBufferId;
        while (buffers.containsKey(idx)) idx++;
        nextBufferId = idx;
        return idx;
    }

    private void addBuffer(GpuTextBuffer page) {
        buffers.put(page.id, page);
    }

    private int removeBuffers(int[] ids) {
        int count = 0;
        for (int id : ids) {
            if (buffers.remove(id) != null) {
                if (screenInstance instanceof VideoRamRasterizer rasterizer) {
                    rasterizer.removeBuffer(node.address(), id);
                }
                if (id == bufferIndex) {
                    bufferIndex = 0;
                }
                count++;
            }
        }
        return count;
    }

    private void removeAllBuffers() {
        if (screenInstance instanceof VideoRamRasterizer rasterizer) {
            rasterizer.removeAllBuffers(node.address());
        }
        buffers.clear();
    }

    private boolean resolveInvokeCosts(int idx, Context context, double budgetCost, int units, double factor) {
        if (idx == 0) {
            context.consumeCallBudget(budgetCost);
            return node.tryChangeBuffer(-units * factor);
        }
        return true;
    }

    private double determineBitbltBudgetCost(TextBuffer dst, TextBuffer src) {
        if (src instanceof GpuTextBuffer page) {
            if (dst instanceof GpuTextBuffer) {
                return 0.0;
            } else if (page.dirty) {
                return bitbltCost * (src.getWidth() * src.getHeight()) / (maxResolution[0] * maxResolution[1]);
            } else {
                return 0.001;
            }
        }
        return 0.0;
    }

    private double determineBitbltEnergyCost(TextBuffer dst) {
        return dst instanceof GpuTextBuffer ? 0 : Settings.get().gpuCopyCost / 15;
    }

    @Callback(direct = true, doc = "function():number -- returns the index of the currently selected buffer.")
    public Object[] getActiveBuffer(Context context, Arguments args) {
        return ResultWrapper.result((double) bufferIndex);
    }

    @Callback(direct = true, doc = "function(index:number):number -- Sets the active buffer. Returns the previously active buffer index.")
    public Object[] setActiveBuffer(Context context, Arguments args) {
        int previousIndex = bufferIndex;
        int newIndex = args.checkInteger(0);
        if (newIndex != 0 && getBuffer(newIndex) == null) {
            return ResultWrapper.result(null, "invalid buffer index");
        }
        bufferIndex = newIndex;
        if (bufferIndex == 0) {
            screen(s -> ResultWrapper.result(true));
        }
        return ResultWrapper.result((double) previousIndex);
    }

    @Callback(direct = true, doc = "function():number[] -- Returns array of indexes of allocated buffers.")
    public Object[] buffers(Context context, Arguments args) {
        return ResultWrapper.result((Object) bufferIndexes());
    }

    @Callback(direct = true, doc = "function([width:number, height:number]):number -- allocates a new buffer.")
    public Object[] allocateBuffer(Context context, Arguments args) {
        int width = args.optInteger(0, maxResolution[0]);
        int height = args.optInteger(1, maxResolution[1]);
        int size = width * height;
        if (width <= 0 || height <= 0)
            return ResultWrapper.result(null, "invalid page dimensions: must be greater than zero");
        if (size > (totalVRAM - calculateUsedMemory()))
            return ResultWrapper.result(null, "not enough video memory");

        PackedColor.ColorFormat format = PackedColor.Depth.format(Settings.screenDepthsByTier[tier]);
        li.cil.oc.core.impl.util.TextBuffer buffer = new li.cil.oc.core.impl.util.TextBuffer(width, height, format);
        int idx = nextAvailableBufferIndex();
        GpuTextBuffer page = GpuTextBuffer.wrap(node.address(), idx, buffer);
        addBuffer(page);
        return ResultWrapper.result((double) page.id);
    }

    @Callback(direct = true, doc = "function(index:number):boolean -- Closes buffer at index. Returns true if a buffer closed. If the active buffer is closed, index moves to 0.")
    public Object[] freeBuffer(Context context, Arguments args) {
        int index = args.optInteger(0, bufferIndex);
        if (removeBuffers(new int[]{index}) == 1) return ResultWrapper.result(true);
        return ResultWrapper.result(null, "no buffer at index");
    }

    @Callback(direct = true, doc = "function():number -- Closes all buffers and returns the count.")
    public Object[] freeAllBuffers(Context context, Arguments args) {
        int count = bufferIndexes().length;
        removeAllBuffers();
        if (bufferIndex != 0) bufferIndex = 0;
        return ResultWrapper.result((double) count);
    }

    @Callback(direct = true, doc = "function():number -- Returns the total VRAM size.")
    public Object[] totalMemory(Context context, Arguments args) {
        return ResultWrapper.result(totalVRAM);
    }

    @Callback(direct = true, doc = "function():number -- Returns the free VRAM not allocated to buffers.")
    public Object[] freeMemory(Context context, Arguments args) {
        return ResultWrapper.result(totalVRAM - calculateUsedMemory());
    }

    @Callback(direct = true, doc = "function(index:number):number, number -- Returns the buffer size at index. Returns screen resolution for index 0.")
    public Object[] getBufferSize(Context context, Arguments args) {
        int idx = args.optInteger(0, bufferIndex);
        return screen(idx, s -> ResultWrapper.result((double) s.getWidth(), (double) s.getHeight()));
    }

    @Callback(direct = true, doc = "function([dst:number, col:number, row:number, width:number, height:number, src:number, fromCol:number, fromRow:number]):boolean -- Bitblt from buffer to screen.")
    public Object[] bitblt(Context context, Arguments args) {
        int dstIdx = args.optInteger(0, 0);
        return screen(dstIdx, dst -> {
            int col = args.optInteger(1, 1);
            int row = args.optInteger(2, 1);
            int w = args.optInteger(3, dst.getWidth());
            int h = args.optInteger(4, dst.getHeight());
            int srcIdx = args.optInteger(5, bufferIndex);
            return screen(srcIdx, src -> {
                int fromCol = args.optInteger(6, 1);
                int fromRow = args.optInteger(7, 1);

                double budgetCost = determineBitbltBudgetCost(dst, src);
                double energyCost = determineBitbltEnergyCost(dst);
                double tierCredit = (tier + 1) * 0.5;
                double overBudget = budgetCost - tierCredit;

                if (overBudget > 0) {
                    if (budgetExhausted) {
                        if (overBudget > tierCredit) {
                            double pauseNeeded = overBudget - tierCredit;
                            double seconds = (pauseNeeded / tierCredit) / 20;
                            context.pause(seconds);
                        }
                        budgetCost = 0;
                    } else {
                        budgetExhausted = true;
                        throw new LimitReachedException();
                    }
                }
                budgetExhausted = false;

                if (resolveInvokeCosts(dstIdx, context, budgetCost, w * h, energyCost)) {
                    if (dstIdx == srcIdx) {
                        int tx = col - fromCol;
                        int ty = row - fromRow;
                        dst.copy(fromCol - 1, fromRow - 1, w, h, tx, ty);
                    } else {
                        GpuTextBuffer.bitblt(dst, col, row, w, h, src, fromCol, fromRow);
                    }
                    return ResultWrapper.result(true);
                }
                return ResultWrapper.result(null, "not enough energy");
            });
        });
    }

    @Callback(doc = "function(address:string[, reset:boolean=true]):boolean -- Binds the GPU to the screen with the specified address.")
    public Object[] bind(Context context, Arguments args) {
        String address = args.checkString(0);
        boolean reset = args.optBoolean(1, true);
        Node screenNode = node.network().node(address);
        if (screenNode == null) {
            return ResultWrapper.result(null, "invalid address");
        }
        if (screenNode.host() instanceof TextBuffer) {
            screenAddress = address;
            screenInstance = (TextBuffer) screenNode.host();
            return screen(s -> {
                if (reset) {
                    if (s instanceof VideoRamRasterizer rasterizer) {
                        rasterizer.removeAllBuffers();
                    }
                    int gmw = maxResolution[0];
                    int gmh = maxResolution[1];
                    int smw = s.getMaximumWidth();
                    int smh = s.getMaximumHeight();
                    s.setResolution(Math.min(gmw, smw), Math.min(gmh, smh));
                    s.setColorDepth(TextBuffer.ColorDepth.values()[Math.min(maxDepth.ordinal(), s.getMaximumColorDepth().ordinal())]);
                    s.setForegroundColor(0xFFFFFF);
                    s.setBackgroundColor(0x000000);
                } else {
                    context.pause(0);
                }
                return ResultWrapper.result(true);
            });
        }
        return ResultWrapper.result(null, "not a screen");
    }

    @Callback(direct = true, doc = "function():string -- Gets the address of the bound screen.")
    public Object[] getScreen(Context context, Arguments args) {
        return screen(0, s -> ResultWrapper.result(s.node().address()));
    }

    @Callback(direct = true, doc = "function():number, boolean -- Gets current background color and whether from palette.")
    public Object[] getBackground(Context context, Arguments args) {
        return screen(bufferIndex, s -> ResultWrapper.result(s.getBackgroundColor(), s.isBackgroundFromPalette()));
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the background color.")
    public Object[] setBackground(Context context, Arguments args) {
        int color = args.checkInteger(0);
        if (bufferIndex == 0) {
            context.consumeCallBudget(setBackgroundCosts[tier]);
        }
        return screen(s -> {
            int oldValue = s.getBackgroundColor();
            Object oldColor, oldIndex;
            if (s.isBackgroundFromPalette()) {
                oldColor = s.getPaletteColor(oldValue);
                oldIndex = (double) oldValue;
            } else {
                oldColor = oldValue;
                oldIndex = null;
            }
            s.setBackgroundColor(color, args.optBoolean(1, false));
            return ResultWrapper.result(oldColor, oldIndex);
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Gets current foreground color and whether from palette.")
    public Object[] getForeground(Context context, Arguments args) {
        return screen(bufferIndex, s -> ResultWrapper.result(s.getForegroundColor(), s.isForegroundFromPalette()));
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color.")
    public Object[] setForeground(Context context, Arguments args) {
        int color = args.checkInteger(0);
        if (bufferIndex == 0) {
            context.consumeCallBudget(setForegroundCosts[tier]);
        }
        return screen(s -> {
            int oldValue = s.getForegroundColor();
            Object oldColor, oldIndex;
            if (s.isForegroundFromPalette()) {
                oldColor = s.getPaletteColor(oldValue);
                oldIndex = (double) oldValue;
            } else {
                oldColor = oldValue;
                oldIndex = null;
            }
            s.setForegroundColor(color, args.optBoolean(1, false));
            return ResultWrapper.result(oldColor, oldIndex);
        });
    }

    @Callback(direct = true, doc = "function(index:number):number -- Gets the palette color at the specified index.")
    public Object[] getPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        return screen(s -> {
            try {
                return ResultWrapper.result(s.getPaletteColor(index));
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function(index:number, color:number):number -- Sets the palette color at the specified index. Returns the previous value.")
    public Object[] setPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        int color = args.checkInteger(1);
        if (bufferIndex == 0) {
            context.consumeCallBudget(setPaletteColorCosts[tier]);
            context.pause(0.1);
        }
        return screen(s -> {
            try {
                int oldColor = s.getPaletteColor(index);
                s.setPaletteColor(index, color);
                return ResultWrapper.result((double) oldColor);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the currently set color depth in bits.")
    public Object[] getDepth(Context context, Arguments args) {
        return screen(s -> ResultWrapper.result((double) PackedColor.Depth.bits(s.getColorDepth())));
    }

    @Callback(doc = "function(depth:number):number -- Sets the color depth. Returns the previous value.")
    public Object[] setDepth(Context context, Arguments args) {
        int depth = args.checkInteger(0);
        return screen(s -> {
            TextBuffer.ColorDepth oldDepth = s.getColorDepth();
            switch (depth) {
                case 1 -> s.setColorDepth(TextBuffer.ColorDepth.OneBit);
                case 4 -> {
                    if (maxDepth.ordinal() >= TextBuffer.ColorDepth.FourBit.ordinal())
                        s.setColorDepth(TextBuffer.ColorDepth.FourBit);
                    else throw new IllegalArgumentException("unsupported depth");
                }
                case 8 -> {
                    if (maxDepth.ordinal() >= TextBuffer.ColorDepth.EightBit.ordinal())
                        s.setColorDepth(TextBuffer.ColorDepth.EightBit);
                    else throw new IllegalArgumentException("unsupported depth");
                }
                default -> throw new IllegalArgumentException("unsupported depth");
            }
            return ResultWrapper.result((double) PackedColor.Depth.bits(oldDepth));
        });
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum supported color depth in bits.")
    public Object[] maxDepth(Context context, Arguments args) {
        return screen(s -> ResultWrapper.result((double) PackedColor.Depth.bits(TextBuffer.ColorDepth.values()[Math.min(maxDepth.ordinal(), s.getMaximumColorDepth().ordinal())])));
    }

    @Callback(direct = true, doc = "function():number, number -- Gets the current screen resolution.")
    public Object[] getResolution(Context context, Arguments args) {
        return screen(s -> ResultWrapper.result((double) s.getWidth(), (double) s.getHeight()));
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Sets the screen resolution.")
    public Object[] setResolution(Context context, Arguments args) {
        int w = args.checkInteger(0);
        int h = args.checkInteger(1);
        int mw = maxResolution[0];
        int mh = maxResolution[1];
        if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
            throw new IllegalArgumentException("unsupported resolution");
        return screen(s -> {
            boolean changed = s.setResolution(w, h);
            return ResultWrapper.result(changed);
        });
    }

    @Callback(direct = true, doc = "function():number, number -- Gets the maximum supported resolution.")
    public Object[] maxResolution(Context context, Arguments args) {
        return screen(s -> {
            int gmw = maxResolution[0];
            int gmh = maxResolution[1];
            int smw = s.getMaximumWidth();
            int smh = s.getMaximumHeight();
            return ResultWrapper.result((double) Math.min(gmw, smw), (double) Math.min(gmh, smh));
        });
    }

    @Callback(direct = true, doc = "function():number, number -- Gets the current viewport size.")
    public Object[] getViewport(Context context, Arguments args) {
        return screen(s -> ResultWrapper.result((double) s.getViewportWidth(), (double) s.getViewportHeight()));
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Sets the viewport size.")
    public Object[] setViewport(Context context, Arguments args) {
        int w = args.checkInteger(0);
        int h = args.checkInteger(1);
        int mw = maxResolution[0];
        int mh = maxResolution[1];
        if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
            throw new IllegalArgumentException("unsupported viewport size");
        return screen(s -> {
            if (w > s.getWidth() || h > s.getHeight())
                throw new IllegalArgumentException("unsupported viewport size");
            return ResultWrapper.result(s.setViewport(w, h));
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number):string, number, number, number or nil, number or nil -- Gets the character and colors at the specified position.")
    public Object[] get(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        return screen(s -> {
            int fgValue = s.getForegroundColor(x, y);
            Object fgColor, fgIndex;
            if (s.isForegroundFromPalette(x, y)) {
                fgColor = s.getPaletteColor(fgValue);
                fgIndex = (double) fgValue;
            } else {
                fgColor = fgValue;
                fgIndex = null;
            }

            int bgValue = s.getBackgroundColor(x, y);
            Object bgColor, bgIndex;
            if (s.isBackgroundFromPalette(x, y)) {
                bgColor = s.getPaletteColor(bgValue);
                bgIndex = (double) bgValue;
            } else {
                bgColor = bgValue;
                bgIndex = null;
            }

            return ResultWrapper.result(new StringBuilder().appendCodePoint(s.getCodePoint(x, y)).toString(), fgColor, bgColor, fgIndex, bgIndex);
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Writes a string to the screen at the specified position.")
    public Object[] set(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        String value = args.checkString(2);
        boolean vertical = args.optBoolean(3, false);
        return screen(s -> {
            if (resolveInvokeCosts(bufferIndex, context, setCosts[tier], ExtendedUnicodeHelper.length(value), Settings.get().gpuSetCost)) {
                s.set(x, y, value, vertical);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(null, "not enough energy");
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a portion of the screen.")
    public Object[] copy(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        int w = Math.max(0, args.checkInteger(2));
        int h = Math.max(0, args.checkInteger(3));
        int tx = args.checkInteger(4);
        int ty = args.checkInteger(5);
        return screen(s -> {
            if (resolveInvokeCosts(bufferIndex, context, copyCosts[tier], w * h, Settings.get().gpuCopyCost)) {
                s.copy(x, y, w, h, tx, ty);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(null, "not enough energy");
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a portion of the screen with the specified character.")
    public Object[] fill(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        int w = Math.max(0, args.checkInteger(2));
        int h = Math.max(0, args.checkInteger(3));
        String value = args.checkString(4);
        if (ExtendedUnicodeHelper.length(value) == 1) {
            return screen(s -> {
                int c = value.codePointAt(0);
                double cost = (c == ' ') ? Settings.get().gpuClearCost : Settings.get().gpuFillCost;
                if (resolveInvokeCosts(bufferIndex, context, fillCosts[tier], w * h, cost)) {
                    s.fill(x, y, w, h, c);
                    return ResultWrapper.result(true);
                }
                return ResultWrapper.result(null, "not enough energy");
            });
        }
        throw new IllegalArgumentException("invalid fill value");
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (node.isNeighborOf(message.source())) {
            if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
                bufferIndex = 0;
                removeAllBuffers();
            }
        }
        if ("computer.stopped".equals(message.name()) && node.isNeighborOf(message.source())) {
            screen(s -> {
                int w = Math.min(maxResolution[0], s.getMaximumWidth());
                int h = Math.min(maxResolution[1], s.getMaximumHeight());
                s.setResolution(w, h);
                s.setColorDepth(TextBuffer.ColorDepth.values()[Math.min(maxDepth.ordinal(), s.getMaximumColorDepth().ordinal())]);
                s.setForegroundColor(0xFFFFFF);
                w = s.getWidth();
                h = s.getHeight();
                if (message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
                    if (machine.lastError() != null) {
                        if (s.getColorDepth().ordinal() > TextBuffer.ColorDepth.OneBit.ordinal())
                            s.setBackgroundColor(0x0000FF);
                        else s.setBackgroundColor(0x000000);
                        s.fill(0, 0, w, h, 0x20);
                        try {
                            String errMsg = localizeError(machine.lastError()).replace("\t", "  ") + "\n";
                            Pattern wrapRegEx = Pattern.compile("(.{1," + Math.max(1, w - 2) + "})\\s");
                            java.util.regex.Matcher matcher = wrapRegEx.matcher(errMsg);
                            StringBuilder sb = new StringBuilder();
                            while (matcher.find()) {
                                matcher.appendReplacement(sb, matcher.group(1) + "\n");
                            }
                            matcher.appendTail(sb);
                            String[] lines = sb.toString().split("\n");
                            int firstRow = Math.max((h - lines.length) / 2, 2);
                            String msg = "Unrecoverable Error";
                            s.set((w - msg.length()) / 2, firstRow - 2, msg, false);
                            int maxLineLength = 0;
                            for (String line : lines) maxLineLength = Math.max(maxLineLength, line.length());
                            int col = Math.max((w - maxLineLength) / 2, 0);
                            for (int i = 0; i < lines.length; i++) {
                                s.set(col, firstRow + i, lines[i], false);
                            }
                        } catch (Throwable t) {
                            LOGGER.error("Error rendering GPU buffer overlay", t);
                        }
                        return null;
                    }
                }
                s.setBackgroundColor(0x000000);
                s.fill(0, 0, w, h, 0x20);
                return null;
            });
        }
    }

    protected String localizeError(String errorKey) {
        return net.minecraft.network.chat.Component.translatable(errorKey).getString();
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node.host() instanceof TextBuffer buffer) {
            if (screenInstance == null && screenAddress != null && node.address().equals(screenAddress)) {
                screenInstance = buffer;
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            screenAddress = null;
            screenInstance = null;
        } else if (screenAddress != null && screenAddress.equals(node.address())) {
            screenInstance = null;
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains("screen")) {
            String s = nbt.getString("screen");
            screenAddress = s.isEmpty() ? null : s;
            screenInstance = null;
        }
        if (nbt.contains("bufferIndex")) {
            bufferIndex = nbt.getInt("bufferIndex");
        }
        removeAllBuffers();
        if (node != null && nbt.contains("videoRam")) {
            CompoundTag videoRamNbt = nbt.getCompound("videoRam");
            ListTag nbtPages = videoRamNbt.getList("pages", (new CompoundTag()).getId());
            for (int i = 0; i < nbtPages.size(); i++) {
                CompoundTag nbtPage = nbtPages.getCompound(i);
                int idx = nbtPage.getInt("page_idx");
                CompoundTag data = nbtPage.getCompound("page_data");
                li.cil.oc.core.impl.util.TextBuffer buf = new li.cil.oc.core.impl.util.TextBuffer(1, 1, PackedColor.Depth.format(TextBuffer.ColorDepth.OneBit));
                buf.load(data, provider);
                buffers.put(idx, GpuTextBuffer.wrap(node.address(), idx, buf));
            }
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (screenAddress != null) {
            nbt.putString("screen", screenAddress);
        }
        nbt.putInt("bufferIndex", bufferIndex);
        CompoundTag videoRamNbt = new CompoundTag();
        ListTag nbtPages = new ListTag();
        for (Map.Entry<Integer, GpuTextBuffer> entry : buffers.entrySet()) {
            CompoundTag nbtPage = new CompoundTag();
            nbtPage.putInt("page_idx", entry.getKey());
            CompoundTag data = new CompoundTag();
            entry.getValue().data.save(data, provider);
            nbtPage.put("page_data", data);
            nbtPages.add(nbtPage);
        }
        videoRamNbt.put("pages", nbtPages);
        nbt.put("videoRam", videoRamNbt);
    }
}
