package li.cil.oc.core.impl.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public interface WidgetContainer {
    Map<WidgetContainer, List<Widget>> _widgets = new WeakHashMap<>();

    default List<Widget> widgets() {
        return _widgets.computeIfAbsent(this, k -> new ArrayList<>());
    }

    default <T extends Widget> T addWidget(T widget) {
        widgets().add(widget);
        widget.owner = this;
        return widget;
    }

    default int windowX() {
        return 0;
    }

    default int windowY() {
        return 0;
    }

    default void drawWidgets(GuiGraphics guiGraphics) {
        for (Widget w : widgets()) {
            w.guiGraphics = guiGraphics;
            w.draw();
        }
    }
}
