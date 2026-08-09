package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.platform.TextureUtil;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class TextureImageRenderer implements ImageRenderer {
    private static final java.util.Map<ResourceLocation, ManualTexture> cache = new java.util.HashMap<>();

    private final int width;
    private final int height;
    private final ResourceLocation location;

    @SuppressWarnings("unused")
    public TextureImageRenderer(ResourceLocation location) {
        this.location = location;
        var mc = Minecraft.getInstance();
        synchronized (cache) {
            var existing = cache.get(location);
            if (existing != null) {
                this.width = existing.width;
                this.height = existing.height;
            } else {
                var tex = new ManualTexture(location);
                mc.getTextureManager().register(location, tex);
                cache.put(location, tex);
                this.width = tex.width;
                this.height = tex.height;
            }
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int ignoredMouseX, int ignoredMouseY) {
        graphics.blit(location, 0, 0, 0, 0, width, height, width, height);
    }

    static class ManualTexture extends AbstractTexture {
        final ResourceLocation location;
        int width = 64;
        int height = 64;

        @SuppressWarnings("unused")
        ManualTexture(ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void load(ResourceManager manager) {
            width = 64;
            height = 64;
            InputStream is = null;
            try {
                var resource = manager.getResourceOrThrow(location);
                is = resource.open();
                var bi = ImageIO.read(is);
                if (bi != null) {
                    width = bi.getWidth();
                    height = bi.getHeight();
                    TextureUtil.prepareImage(getId(), 0, width, height);
                    try (var nativeImage = new com.mojang.blaze3d.platform.NativeImage(width, height, false)) {
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                int argb = bi.getRGB(x, y);
                                int a = (argb >> 24) & 0xFF;
                                int r = (argb >> 16) & 0xFF;
                                int g = (argb >> 8) & 0xFF;
                                int b = argb & 0xFF;
                                nativeImage.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                            }
                        }
                        this.bind();
                        nativeImage.upload(0, 0, 0, 0, 0, width, height, false, false, false, true);
                    }
                }
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(TextureImageRenderer.class).warn("Failed to load manual texture: {}", location, e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }
}
