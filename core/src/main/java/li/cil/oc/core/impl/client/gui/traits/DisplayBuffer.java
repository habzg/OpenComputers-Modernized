package li.cil.oc.core.impl.client.gui.traits;

import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public interface DisplayBuffer {
    int bufferX();

    int bufferY();

    int bufferColumns();

    int bufferRows();

    @SuppressWarnings("SameReturnValue")
    default boolean guiSizeChanged() {
        return false;
    }

    default int currentWidth() {
        return -1;
    }

    default int currentHeight() {
        return -1;
    }

    default double scale() {
        return 0.0;
    }

    default void initGui() {
        BufferRenderer.init(Minecraft.getInstance().getTextureManager());
    }

    default void drawBufferLayer(GuiGraphics guiGraphics) {
        int oldWidth = currentWidth();
        int oldHeight = currentHeight();
        changeSize(bufferColumns(), bufferRows(), guiSizeChanged() || oldWidth != bufferColumns() || oldHeight != bufferRows());
    }

    @SuppressWarnings("UnusedReturnValue")
    double changeSize(double w, double h, boolean recompile);
}
