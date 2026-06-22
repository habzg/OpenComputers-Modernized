package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class Screen implements li.cil.oc.neoforge.client.gui.traits.InputBuffer {
    public final li.cil.oc.api.internal.TextBuffer buffer;
    public final boolean hasMouse;
    public final Supplier<Boolean> hasKeyboardCallback;
    public final Supplier<Boolean> hasPower;
    private int x, y = 0;
    private double scale = 1.0;
    private int mx = -1, my = -1;
    private boolean didClick = false;

    public Screen(li.cil.oc.api.internal.TextBuffer buffer, boolean hasMouse, Supplier<Boolean> hasKeyboardCallback, Supplier<Boolean> hasPower) {
        this.buffer = buffer;
        this.hasMouse = hasMouse;
        this.hasKeyboardCallback = hasKeyboardCallback;
        this.hasPower = hasPower;
    }

    @Override
    public li.cil.oc.api.internal.TextBuffer buffer() {
        return buffer;
    }

    @Override
    public boolean hasKeyboard() {
        return hasKeyboardCallback.get();
    }

    @Override
    public int bufferColumns() {
        var b = buffer();
        return b != null ? b.getViewportWidth() : 0;
    }

    @Override
    public int bufferRows() {
        var b = buffer();
        return b != null ? b.getViewportHeight() : 0;
    }

    @Override
    public int bufferX() {
        return 8 + x;
    }

    @Override
    public int bufferY() {
        return 8 + y;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!hasMouse || scrollDelta == 0) return true;
        var b = buffer();
        if (b == null) return true;
        double[] bxby = toBufferCoordinates((int) mouseX, (int) mouseY);
        if (bxby != null) b.mouseScroll(bxby[0], bxby[1], (int) Math.signum(scrollDelta), null);
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasMouse && (button == 0 || button == 1)) {
            clickOrDrag((int) mouseX, (int) mouseY, button);
        } else if (button == 2) {
            var b = buffer();
            if (b != null) {
                b.clipboard(net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard(), null);
            } else {
                showKeyboardMissing(System.currentTimeMillis());
            }
        }
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean mouseDragged(double mouseX, double mouseY, int button, double ignoredDragX, double ignoredDragY) {
        if (hasMouse && (button == 0 || button == 1)) {
            clickOrDrag((int) mouseX, (int) mouseY, button);
        }
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!hasMouse || button < 0) return true;
        var b = buffer();
        if (b == null) return true;
        if (didClick) {
            double[] bxby = toBufferCoordinates((int) mouseX, (int) mouseY);
            if (bxby != null) b.mouseUp(bxby[0], bxby[1], button, null);
            else b.mouseUp(-1.0, -1.0, button, null);
        }
        didClick = false;
        mx = -1;
        my = -1;
        return true;
    }

    private void clickOrDrag(int mouseX, int mouseY, int button) {
        var b = buffer();
        if (b == null) return;
        double[] bxby = toBufferCoordinates(mouseX, mouseY);
        if (bxby != null) {
            if ((int) bxby[0] != mx || (int) (bxby[1] * 2) != my) {
                if (mx >= 0 && my >= 0) b.mouseDrag(bxby[0], bxby[1], button, null);
                else b.mouseDown(bxby[0], bxby[1], button, null);
                didClick = true;
                mx = (int) bxby[0];
                my = (int) (bxby[1] * 2);
            }
        }
    }

    private double[] toBufferCoordinates(int mouseX, int mouseY) {
        var b = buffer();
        if (b == null) return null;
        int bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin;
        double bx = (mouseX - x - bufferMargin) / scale() / TextBufferRenderCache.renderer.charRenderWidth();
        double by = (mouseY - y - bufferMargin) / scale() / TextBufferRenderCache.renderer.charRenderHeight();
        int bw = b.getViewportWidth();
        int bh = b.getViewportHeight();
        if (bx >= 0 && by >= 0 && bx < bw && by < bh) return new double[]{bx, by};
        return null;
    }

    public void render(GuiGraphics guiGraphics, int ignoredMouseX, int ignoredMouseY, float ignoredDt) {
        var b = buffer();
        if (b == null) return;
        int oldWidth = currentWidth();
        int oldHeight = currentHeight();
        changeSize(bufferColumns(), bufferRows(), guiSizeChanged() || oldWidth != bufferColumns() || oldHeight != bufferRows());
        drawGui(guiGraphics);
    }

    public void drawGui(GuiGraphics guiGraphics) {
        var b = buffer();
        if (b == null) return;
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        int bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin;
        Matrix4f transform = new Matrix4f(pose.last().pose());
        transform.translate(bufferMargin, bufferMargin, 0);
        transform.scale((float) scale(), (float) scale(), 1);
        BufferRenderer.drawGui(guiGraphics, b,
                b.getViewportWidth(), b.getViewportHeight(),
                transform, hasPower.get(), 1.0f, x, y);
        pose.popPose();
    }

    @Override
    public double scale() {
        return scale;
    }

    private int guiWidth = 320;
    private int guiHeight = 200;

    public void setGuiSize(int width, int height) {
        this.guiWidth = width;
        this.guiHeight = height;
    }

    @Override
    public double changeSize(double w, double h, boolean recompile) {
        var b = buffer();
        if (b == null) return scale;
        double bw = b.renderWidth();
        double bh = b.renderHeight();
        int bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin;
        double scaleX = Math.min(guiWidth / (bw + bufferMargin * 2.0), 1);
        double scaleY = Math.min(guiHeight / (bh + bufferMargin * 2.0), 1);
        scale = Math.min(scaleX, scaleY);
        int innerWidth = (int) (bw * scale);
        int innerHeight = (int) (bh * scale);
        x = (guiWidth - (innerWidth + bufferMargin * 2)) / 2;
        y = (guiHeight - (innerHeight + bufferMargin * 2)) / 2;
        if (recompile) BufferRenderer.compileBackground(innerWidth, innerHeight);
        return scale;
    }
}
