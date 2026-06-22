package li.cil.oc.core.impl.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("unused")
public abstract class Widget {
    public WidgetContainer owner;
    public GuiGraphics guiGraphics;

    @SuppressWarnings("unused")
    public abstract int x();

    @SuppressWarnings("unused")
    public abstract int y();

    @SuppressWarnings("unused")
    public abstract int width();

    @SuppressWarnings("unused")
    public abstract int height();

    public abstract void draw();
}
