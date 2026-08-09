package li.cil.oc.core.impl.client.renderer.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.renderer.IBufferRenderProvider;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.font.DynamicFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.IFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.StaticFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.TextureFontRenderer;
import li.cil.oc.core.impl.util.PackedColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class BufferRenderer {
    public static final int margin = 7;
    public static final int innerMargin = 1;

    private static int bgInnerWidth, bgInnerHeight;
    private static boolean bgForRobot;

    private static IBufferRenderProvider provider = DefaultProvider.INSTANCE;

    public static void setProvider(IBufferRenderProvider p) {
        provider = p;
    }

    public static void init(TextureManager tm) {
        Textures.init(tm);
    }

    public static void compileBackground(int bufferWidth, int bufferHeight) {
        compileBackground(bufferWidth, bufferHeight, false);
    }

    public static void compileBackground(int bufferWidth, int bufferHeight, boolean forRobot) {
        bgInnerWidth = innerMargin * 2 + bufferWidth;
        bgInnerHeight = innerMargin * 2 + bufferHeight;
        bgForRobot = forRobot;
    }

    public static void drawGui(GuiGraphics guiGraphics, TextBuffer screen,
                               int viewportWidth, int viewportHeight, Matrix4f transform,
                               boolean hasPower, float alpha, int guiX, int guiY) {
        MultiBufferSource buffers = guiGraphics.bufferSource();

        drawGuiBorder(buffers, guiX, guiY);

        if (hasPower && viewportWidth > 0 && viewportHeight > 0) {
            drawGuiText(buffers, screen, viewportWidth, viewportHeight, transform, alpha);
        }
        guiGraphics.flush();
    }

    private static void drawGuiBorder(MultiBufferSource buffers, int guiX, int guiY) {
        final int innerWidth = bgInnerWidth;
        final int innerHeight = bgInnerHeight;
        final boolean forRobot = bgForRobot;

        VertexConsumer consumer = buffers.getBuffer(provider.borderRenderType());

        int m = forRobot ? 2 : 7;
        double c0, c1, c2, c3;
        if (forRobot) {
            c0 = 5;
            c1 = 7;
            c2 = 9;
            c3 = 11;
        } else {
            c0 = 0;
            c1 = 7;
            c2 = 9;
            c3 = 16;
        }

        drawBorderQuad(consumer, guiX, guiY, 0, 0, m, m, c0, c0, c1, c1);
        drawBorderQuad(consumer, guiX, guiY, m, 0, innerWidth, m, c1 + 0.25, c0, c2 - 0.25, c1);
        drawBorderQuad(consumer, guiX, guiY, m + innerWidth, 0, m, m, c2, c0, c3, c1);
        drawBorderQuad(consumer, guiX, guiY, 0, m, m, innerHeight, c0, c1 + 0.25, c1, c2 - 0.25);
        drawBorderQuad(consumer, guiX, guiY, m, m, innerWidth, innerHeight, c1 + 0.25, c1 + 0.25, c2 - 0.25, c2 - 0.25);
        drawBorderQuad(consumer, guiX, guiY, m + innerWidth, m, m, innerHeight, c2, c1 + 0.25, c3, c2 - 0.25);
        drawBorderQuad(consumer, guiX, guiY, 0, m + innerHeight, m, m, c0, c2, c1, c3);
        drawBorderQuad(consumer, guiX, guiY, m, m + innerHeight, innerWidth, m, c1 + 0.25, c2, c2 - 0.25, c3);
        drawBorderQuad(consumer, guiX, guiY, m + innerWidth, m + innerHeight, m, m, c2, c2, c3, c3);
    }

    private static void drawBorderQuad(VertexConsumer consumer, int guiX, int guiY,
                                       double x, double y, double w, double h,
                                       double u1, double v1, double u2, double v2) {
        double u1d = u1 / 16.0;
        double u2d = u2 / 16.0;
        double v1d = v1 / 16.0;
        double v2d = v2 / 16.0;
        consumer.addVertex((float) (guiX + x), (float) (guiY + y + h), 0).setUv((float) u1d, (float) v2d);
        consumer.addVertex((float) (guiX + x + w), (float) (guiY + y + h), 0).setUv((float) u2d, (float) v2d);
        consumer.addVertex((float) (guiX + x + w), (float) (guiY + y), 0).setUv((float) u2d, (float) v1d);
        consumer.addVertex((float) (guiX + x), (float) (guiY + y), 0).setUv((float) u1d, (float) v1d);
    }

    @SuppressWarnings("unused")
    private static void drawGuiText(MultiBufferSource buffers, TextBuffer screen,
                                    int viewportWidth, int viewportHeight, Matrix4f transform, float alpha) {
        li.cil.oc.core.impl.util.TextBuffer data = getTextData(screen);
        if (data == null) return;

        final IFontRenderer renderer = TextBufferRenderCache.renderer;
        final int charWidth = renderer.charRenderWidth() * 2;
        final int charHeight = renderer.charRenderHeight() * 2;
        final PackedColor.ColorFormat format = data.format();

        final Matrix4f localTransform = new Matrix4f(transform);
        localTransform.scale(0.5f, 0.5f, 1.0f);

        final boolean hasBackground = drawGuiTextBackground(buffers, data, viewportWidth, viewportHeight,
                localTransform, charWidth, charHeight, format, alpha);

        int fullBright = 240 << 4 | 240 << 20;

        if (renderer instanceof DynamicFontRenderer dfr) {
            for (int i = 0; i < dfr.textureCount(); i++) {
                ResourceLocation loc = dfr.getFontTextureLocation(i);
                VertexConsumer consumer = buffers.getBuffer(provider.textRenderType(loc));

                for (int y = 0; y < Math.min(viewportHeight, data.height); y++) {
                    final int[] line = data.buffer[y];
                    final short[] colorRow = data.color[y];
                    final float ty = y * charHeight;

                    final int cols = Math.min(viewportWidth, line.length);
                    for (int n = 0; n < cols; n++) {
                        final int ch = line[n];
                        if (ch == ' ') continue;
                        final int col = PackedColor.unpackForeground(colorRow[n], format);
                        final float r = ((col >> 16) & 0xFF) / 255f;
                        final float g = ((col >> 8) & 0xFF) / 255f;
                        final float b = (col & 0xFF) / 255f;

                        TextureFontRenderer.CharRenderInfo info = dfr.getCharRenderInfo(ch);
                        if (info != null && info.textureIndex() == i) {
                            drawCharQuad(consumer, localTransform, n * charWidth, ty,
                                    info, r, g, b, alpha, fullBright);
                        }
                    }
                }
            }
        } else if (renderer instanceof StaticFontRenderer sfr) {
            ResourceLocation loc = sfr.getFontTextureLocation(0);
            VertexConsumer consumer = buffers.getBuffer(provider.textRenderType(loc));

            for (int y = 0; y < Math.min(viewportHeight, data.height); y++) {
                final int[] line = data.buffer[y];
                final short[] colorRow = data.color[y];
                final float ty = y * charHeight;

                final int cols = Math.min(viewportWidth, line.length);
                for (int n = 0; n < cols; n++) {
                    final int ch = line[n];
                    if (ch == ' ') continue;
                    final int col = PackedColor.unpackForeground(colorRow[n], format);
                    final float r = ((col >> 16) & 0xFF) / 255f;
                    final float g = ((col >> 8) & 0xFF) / 255f;
                    final float b = (col & 0xFF) / 255f;

                    TextureFontRenderer.CharRenderInfo info = sfr.getCharRenderInfo(ch);
                    if (info != null) {
                        drawCharQuad(consumer, localTransform, n * charWidth, ty,
                                info, r, g, b, alpha, fullBright);
                    }
                }
            }
        }
    }

    private static boolean drawGuiTextBackground(MultiBufferSource buffers,
                                                 li.cil.oc.core.impl.util.TextBuffer data,
                                                 int viewportWidth, int viewportHeight,
                                                 Matrix4f localTransform,
                                                 int charWidth, int charHeight,
                                                 PackedColor.ColorFormat format, float alpha) {
        VertexConsumer consumer = buffers.getBuffer(provider.backgroundRenderType());

        boolean hasBg = false;
        for (int y = 0; y < Math.min(viewportHeight, data.height); y++) {
            final short[] colorRow = data.color[y];
            int cbg = 0x000000;
            int x = 0;
            int width = 0;
            for (int col = 0; col < colorRow.length && x + width < viewportWidth; col++) {
                int bg = PackedColor.unpackBackground(colorRow[col], format);
                if (bg != cbg) {
                    if (cbg != 0 && width > 0) {
                        drawBackgroundQuad(consumer, localTransform, cbg, x, y, width, charWidth, charHeight, alpha);
                        hasBg = true;
                    }
                    cbg = bg;
                    x += width;
                    width = 0;
                }
                width++;
            }
            if (cbg != 0 && width > 0) {
                drawBackgroundQuad(consumer, localTransform, cbg, x, y, width, charWidth, charHeight, alpha);
                hasBg = true;
            }
        }
        return hasBg;
    }

    private static void drawBackgroundQuad(VertexConsumer consumer, Matrix4f m, int color,
                                           int x, int y, int width, int charWidth, int charHeight, float alpha) {
        final float x0 = x * charWidth;
        final float x1 = (x + width) * charWidth;
        final float y0 = y * charHeight;
        final float y1 = (y + 1) * charHeight;
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        final Vector4f p0 = new Vector4f(x0, y1, 0, 1.0f).mul(m);
        final Vector4f p1 = new Vector4f(x1, y1, 0, 1.0f).mul(m);
        final Vector4f p2 = new Vector4f(x1, y0, 0, 1.0f).mul(m);
        final Vector4f p3 = new Vector4f(x0, y0, 0, 1.0f).mul(m);

        float a = Math.clamp(alpha, 0.0f, 1.0f);
        consumer.addVertex(p0.x(), p0.y(), p0.z()).setColor(r, g, b, a);
        consumer.addVertex(p1.x(), p1.y(), p1.z()).setColor(r, g, b, a);
        consumer.addVertex(p2.x(), p2.y(), p2.z()).setColor(r, g, b, a);
        consumer.addVertex(p3.x(), p3.y(), p3.z()).setColor(r, g, b, a);
    }

    private static void drawCharQuad(VertexConsumer consumer, Matrix4f m, float tx, float ty,
                                     TextureFontRenderer.CharRenderInfo info,
                                     float r, float g, float b, float alpha, int lightmap) {
        final Vector4f p0 = new Vector4f(tx, ty + info.height(), 0, 1.0f).mul(m);
        final Vector4f p1 = new Vector4f(tx + info.width(), ty + info.height(), 0, 1.0f).mul(m);
        final Vector4f p2 = new Vector4f(tx + info.width(), ty, 0, 1.0f).mul(m);
        final Vector4f p3 = new Vector4f(tx, ty, 0, 1.0f).mul(m);

        float a = Math.clamp(alpha, 0.0f, 1.0f);
        consumer.addVertex(p0.x(), p0.y(), p0.z()).setColor(r, g, b, a).setUv(info.u1(), info.v2()).setLight(lightmap);
        consumer.addVertex(p1.x(), p1.y(), p1.z()).setColor(r, g, b, a).setUv(info.u2(), info.v2()).setLight(lightmap);
        consumer.addVertex(p2.x(), p2.y(), p2.z()).setColor(r, g, b, a).setUv(info.u2(), info.v1()).setLight(lightmap);
        consumer.addVertex(p3.x(), p3.y(), p3.z()).setColor(r, g, b, a).setUv(info.u1(), info.v1()).setLight(lightmap);
    }

    private static li.cil.oc.core.impl.util.TextBuffer getTextData(TextBuffer screen) {
        if (screen instanceof li.cil.oc.core.impl.common.component.TextBufferBase base) {
            return base.data;
        }
        return null;
    }

    private static final class DefaultProvider implements IBufferRenderProvider {
        static final IBufferRenderProvider INSTANCE = new DefaultProvider();
        private static final RenderType DEFAULT_BG = RenderType.gui();

        @Override
        public RenderType borderRenderType() {
            return RenderType.gui();
        }

        @Override
        public RenderType backgroundRenderType() {
            return DEFAULT_BG;
        }

        @Override
        public RenderType textRenderType(ResourceLocation texture) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
