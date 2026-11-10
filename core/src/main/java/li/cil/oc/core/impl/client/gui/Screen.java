package li.cil.oc.core.impl.client.gui;

import java.util.function.Supplier;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.core.impl.client.gui.traits.InputBuffer;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

public class Screen implements InputBuffer {
  private final TextBuffer buffer;
  private final boolean hasMouse;
  private final Supplier<Boolean> hasKeyboardCallback;
  private final Supplier<Boolean> hasPower;
  private int x, y = 0;
  private double scale = 1.0;
  private int mx = -1, my = -1;
  private boolean didClick = false;
  private long mousePressTime = 0;

  public Screen(TextBuffer buffer, boolean hasMouse, Supplier<Boolean> hasKeyboardCallback, Supplier<Boolean> hasPower) {
    this.buffer = buffer;
    this.hasMouse = hasMouse;
    this.hasKeyboardCallback = hasKeyboardCallback;
    this.hasPower = hasPower;
  }

  @Override
  public TextBuffer buffer() {
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
      mousePressTime = System.currentTimeMillis();
      clickOrDrag((int) mouseX, (int) mouseY, button);
    } else if (button == 2) {
      var b = buffer();
      if (b != null && hasKeyboard()) {
        b.clipboard(net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard(), null);
      } else {
        showKeyboardMissing(System.currentTimeMillis());
      }
    }
    return true;
  }

  @SuppressWarnings("SameReturnValue")
  public boolean mouseDragged(double mouseX, double mouseY, int button, double ignoredDragX, double ignoredDragY) {
    if (hasMouse && (button == 0 || button == 1) && System.currentTimeMillis() - mousePressTime > 10) {
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
    hasPower.get();
    var b = buffer();
    if (b == null) return;
    int oldWidth = currentWidth();
    int oldHeight = currentHeight();
    int cols = bufferColumns();
    int rows = bufferRows();
    changeSize(cols, rows, guiSizeChanged() || oldWidth != cols || oldHeight != rows);
    currentWidth = cols;
    currentHeight = rows;
    guiSizeChanged = false;
    drawGui(guiGraphics);
    drawBufferLayer(guiGraphics);
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
  private int currentWidth = -1;
  private int currentHeight = -1;
  private boolean guiSizeChanged = false;

  @Override
  public int currentWidth() {
    return currentWidth;
  }

  @Override
  public int currentHeight() {
    return currentHeight;
  }

  @Override
  public boolean guiSizeChanged() {
    return guiSizeChanged;
  }

  @Override
  public void initGui() {
    BufferRenderer.init(Minecraft.getInstance().getTextureManager());
    guiSizeChanged = true;
  }

  public void setGuiSize(int width, int height) {
    if (this.guiWidth != width || this.guiHeight != height) {
      this.guiWidth = width;
      this.guiHeight = height;
      guiSizeChanged = true;
    }
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
