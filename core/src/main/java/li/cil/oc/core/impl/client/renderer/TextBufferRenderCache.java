package li.cil.oc.core.impl.client.renderer;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.client.renderer.font.DynamicFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.IFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.StaticFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.TextBufferRenderData;

public final class TextBufferRenderCache {
    public static final IFontRenderer renderer =
            "texture".equalsIgnoreCase(Settings.get().fontRenderer) ? new StaticFontRenderer() : new DynamicFontRenderer();

    public static void generateChars(TextBufferRenderData buffer) {
        if (buffer.dirty()) {
            for (var line : buffer.data().buffer) {
                renderer.generateChars(line);
            }
            buffer.setDirty(false);
        }
    }
}
