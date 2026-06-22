package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

public class TextureImageRenderer implements ImageRenderer {
    private final int width;
    private final int height;
    private final ResourceLocation location;

    @SuppressWarnings("unused")
    public TextureImageRenderer(ResourceLocation location) {
        this.location = location;
        var mc = Minecraft.getInstance();
        var manager = mc.getTextureManager();
        var existing = manager.getTexture(location);
        if (existing instanceof ManualTexture tex) {
            this.width = tex.width;
            this.height = tex.height;
        } else {
            var tex = new ManualTexture(location);
            manager.register(location, tex);
            this.width = tex.width;
            this.height = tex.height;
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
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int mouseX, int mouseY) {
        var m = poseStack.last().pose();
        var consumer = bufferSource.getBuffer(RenderType.entityCutout(location));
        consumer.addVertex(m, 0, 0, 0).setColor(1, 1, 1, 1).setUv(0, 0).setLight(0x00F000F0).setNormal(0, 0, 1);
        consumer.addVertex(m, 0, height, 0).setColor(1, 1, 1, 1).setUv(0, 1).setLight(0x00F000F0).setNormal(0, 0, 1);
        consumer.addVertex(m, width, height, 0).setColor(1, 1, 1, 1).setUv(1, 1).setLight(0x00F000F0).setNormal(0, 0, 1);
        consumer.addVertex(m, width, 0, 0).setColor(1, 1, 1, 1).setUv(1, 0).setLight(0x00F000F0).setNormal(0, 0, 1);
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
            } catch (IOException ignored) {
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
