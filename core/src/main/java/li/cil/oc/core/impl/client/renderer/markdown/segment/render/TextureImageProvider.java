package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.core.impl.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TextureImageProvider implements ImageProvider {
    @Override
    public ImageRenderer getImage(String data) {
        try {
            return new TextureImageRenderer(ResourceLocation.parse(data));
        } catch (Throwable t) {
            return new InteractiveImageRenderer() {
                @Override
                public String getTooltip(String tooltip) {
                    return "gui.opencomputers.manual.warning.imagemissing";
                }

                @Override
                public boolean onMouseClick(int mouseX, int mouseY) {
                    return false;
                }

                @Override
                public int getWidth() {
                    return 64;
                }

                @Override
                public int getHeight() {
                    return 64;
                }

                @Override
                public void render(GuiGraphics graphics, int mouseX, int mouseY) {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    mc.getTextureManager().getTexture(Textures.guiManualMissingItem);
                    new TextureImageRenderer(Textures.guiManualMissingItem).render(graphics, mouseX, mouseY);
                }
            };
        }
    }
}
