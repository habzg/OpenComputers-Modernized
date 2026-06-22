package li.cil.oc.core.impl.common.component;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.common.component.traits.TextBufferProxy;
import li.cil.oc.core.impl.common.component.traits.VideoRamRasterizer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class GpuTextBuffer implements TextBufferProxy {
    public final String owner;
    public final int id;
    public final li.cil.oc.core.impl.util.TextBuffer data;

    public boolean dirty = true;

    public GpuTextBuffer(String owner, int id, li.cil.oc.core.impl.util.TextBuffer data) {
        this.owner = owner;
        this.id = id;
        this.data = data;
    }

    public static GpuTextBuffer wrap(String owner, int id, li.cil.oc.core.impl.util.TextBuffer data) {
        return new GpuTextBuffer(owner, id, data);
    }

    public static void bitblt(li.cil.oc.api.internal.TextBuffer dst, int col, int row, int w, int h,
                              li.cil.oc.api.internal.TextBuffer src, int fromCol, int fromRow) {
        int x = col - 1;
        int y = row - 1;
        int fx = fromCol - 1;
        int fy = fromRow - 1;
        int adjustedDstX = x;
        int adjustedDstY = y;
        int adjustedWidth = w;
        int adjustedHeight = h;
        int adjustedSourceX = fx;
        int adjustedSourceY = fy;

        if (x < 0) {
            adjustedWidth += x;
            adjustedSourceX -= x;
            adjustedDstX = 0;
        }
        if (y < 0) {
            adjustedHeight += y;
            adjustedSourceY -= y;
            adjustedDstY = 0;
        }
        if (adjustedSourceX < 0) {
            adjustedWidth += adjustedSourceX;
            adjustedDstX -= adjustedSourceX;
            adjustedSourceX = 0;
        }
        if (adjustedSourceY < 0) {
            adjustedHeight += adjustedSourceY;
            adjustedDstY -= adjustedSourceY;
            adjustedSourceY = 0;
        }

        adjustedWidth -= Math.max((adjustedDstX + adjustedWidth) - dst.getWidth(), 0);
        adjustedWidth -= Math.max((adjustedSourceX + adjustedWidth) - src.getWidth(), 0);
        adjustedHeight -= Math.max((adjustedDstY + adjustedHeight) - dst.getHeight(), 0);
        adjustedHeight -= Math.max((adjustedSourceY + adjustedHeight) - src.getHeight(), 0);

        if (adjustedWidth <= 0 || adjustedHeight <= 0) return;

        if (dst instanceof GpuTextBuffer dstGpu && src instanceof TextBufferProxy srcProxy) {
            writeToVram(dstGpu, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcProxy, adjustedSourceX, adjustedSourceY);
        } else if (src instanceof GpuTextBuffer srcGpu && dst instanceof TextBufferProxy dstProxy && dstProxy instanceof VideoRamRasterizer rasterizer) {
            if (dstProxy.data().rawcopy(adjustedDstX + 1, adjustedDstY + 1, adjustedWidth, adjustedHeight, srcGpu.data, adjustedSourceX + 1, adjustedSourceY + 1)) {
                rasterizer.addBuffer(srcGpu);
                rasterizer.onBufferBitBlt(adjustedDstX + 1, adjustedDstY + 1, adjustedWidth, adjustedHeight, srcGpu, adjustedSourceX + 1, adjustedSourceY + 1);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported buffer types for bitblt");
        }
    }

    private static void writeToVram(GpuTextBuffer dstRam, int x, int y, int w, int h,
                                    TextBufferProxy src, int fx, int fy) {
        if (dstRam.data.rawcopy(x + 1, y + 1, w, h, src.data(), fx + 1, fy + 1)) {
            dstRam.dirty = true;
        }
    }

    @Override
    public li.cil.oc.core.impl.util.TextBuffer data() {
        return data;
    }

    @Override
    public Node node() {
        throw new RuntimeException("GpuTextBuffers do not have nodes");
    }

    @Override
    public int getMaximumWidth() {
        return data.width;
    }

    @Override
    public int getMaximumHeight() {
        return data.height;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public int getViewportWidth() {
        return data.height;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public int getViewportHeight() {
        return data.width;
    }

    @Override
    public void onBufferSet(int col, int row, String s, boolean vertical) {
        dirty = true;
    }

    @Override
    public void onBufferColorChange() {
        dirty = true;
    }

    @Override
    public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
        dirty = true;
    }

    @Override
    public void onBufferFill(int col, int row, int w, int h, int c) {
        dirty = true;
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        data.load(nbt, provider);
        dirty = true;
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        data.save(nbt, provider);
        dirty = false;
    }

    @Override
    public double getEnergyCostPerTick() {
        return 0;
    }

    @Override
    public void setEnergyCostPerTick(double value) {
    }

    @Override
    public boolean getPowerState() {
        return false;
    }

    @Override
    public void setPowerState(boolean value) {
    }

    @Override
    public void setMaximumResolution(int width, int height) {
    }

    @Override
    public void setAspectRatio(double width, double height) {
    }

    @Override
    public double getAspectRatio() {
        return 1;
    }

    @Override
    public boolean setResolution(int width, int height) {
        return false;
    }

    @Override
    public boolean setViewport(int width, int height) {
        return false;
    }

    @Override
    public TextBuffer.ColorDepth getMaximumColorDepth() {
        return data.format().depth();
    }

    @Override
    public void setMaximumColorDepth(TextBuffer.ColorDepth depth) {
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
        return false;
    }

    @Override
    public void setRenderingEnabled(boolean enabled) {
    }

    @Override
    public void keyDown(char character, int code, Player player) {
    }

    @Override
    public void keyUp(char character, int code, Player player) {
    }

    @Override
    public void clipboard(String value, Player player) {
    }

    @Override
    public void dropFile(String fileName, String fileContent, Player player) {
    }

    @Override
    public void mouseDown(double x, double y, int button, Player player) {
    }

    @Override
    public void mouseDrag(double x, double y, int button, Player player) {
    }

    @Override
    public void mouseUp(double x, double y, int button, Player player) {
    }

    @Override
    public void mouseScroll(double x, double y, int delta, Player player) {
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }

    public static class ClientGpuTextBufferHandler {
        public static void bitblt(li.cil.oc.api.internal.TextBuffer dst, int col, int row, int w, int h,
                                  String owner, int srcId, int fromCol, int fromRow) {
            if (dst instanceof VideoRamRasterizer videoDevice) {
                var buffer = videoDevice.getBuffer(owner, srcId);
                if (buffer instanceof GpuTextBuffer gpuBuffer) {
                    GpuTextBuffer.bitblt(dst, col, row, w, h, gpuBuffer, fromCol, fromRow);
                }
            }
        }

        @SuppressWarnings("UnusedReturnValue")
        public static boolean removeBuffer(li.cil.oc.api.internal.TextBuffer buffer, String owner, int id) {
            if (buffer instanceof VideoRamRasterizer screen) {
                return screen.removeBuffer(owner, id);
            }
            return false;
        }

        @SuppressWarnings("UnusedReturnValue")
        public static boolean loadBuffer(li.cil.oc.api.internal.TextBuffer buffer, String owner, int id, CompoundTag nbt) {
            if (buffer instanceof VideoRamRasterizer screen) {
                return screen.loadBuffer(owner, id, nbt);
            }
            return false;
        }
    }
}
