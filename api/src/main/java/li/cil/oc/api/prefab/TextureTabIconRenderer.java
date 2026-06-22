package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Simple implementation of a tab icon renderer using a full texture as its graphic.
 */

public class TextureTabIconRenderer implements TabIconRenderer {
    private final ResourceLocation location;

    @SuppressWarnings("unused")
    public TextureTabIconRenderer(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        guiGraphics.blit(location, 0, 0, 0, 0, 16, 16, 16, 16);
    }
}
