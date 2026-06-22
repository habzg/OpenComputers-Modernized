package li.cil.oc.neoforge.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.font.DynamicFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.IFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.StaticFontRenderer;
import li.cil.oc.core.impl.client.renderer.font.TextureFontRenderer;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.impl.util.TextBuffer;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.function.Function;

public final class ScreenBufferTextRenderer {
    @SuppressWarnings("unused")
    private ScreenBufferTextRenderer() {
    }

    private static final RenderType BACKGROUND_TYPE = RenderType.create(
            "oc_screen_background",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    private static final Function<ResourceLocation, RenderType> TEXT_TYPE = Util.memoize(texture ->
            RenderType.create(
                    "oc_screen_text",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    786432,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                            .setLightmapState(RenderStateShard.LIGHTMAP)
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                            .createCompositeState(false)
            )
    );

    private static final int FULL_BRIGHT = LightTexture.pack(240, 240);

    @SuppressWarnings("unused")
    public static void render(MultiBufferSource buffers, TextBuffer textBuffer,
                              int viewportWidth, int viewportHeight, Matrix4f transform) {
        render(buffers, textBuffer, viewportWidth, viewportHeight, transform, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void render(MultiBufferSource buffers, TextBuffer textBuffer,
                              int viewportWidth, int viewportHeight, Matrix4f transform, float alpha) {
        render(buffers, textBuffer, viewportWidth, viewportHeight, transform, 1.0f, 1.0f, 1.0f, alpha);
    }

    public static void render(MultiBufferSource buffers, TextBuffer textBuffer,
                              int viewportWidth, int viewportHeight, Matrix4f transform, float tintR, float tintG, float tintB, float alpha) {
        final IFontRenderer renderer = TextBufferRenderCache.renderer;
        final int charWidth = renderer.charRenderWidth() * 2;
        final int charHeight = renderer.charRenderHeight() * 2;
        final PackedColor.ColorFormat format = textBuffer.format();

        final Matrix4f localTransform = new Matrix4f(transform);
        localTransform.scale(0.5f, 0.5f, 1.0f);

        final VertexConsumer bgConsumer = buffers.getBuffer(BACKGROUND_TYPE);

        for (int y = 0; y < Math.min(viewportHeight, textBuffer.height); y++) {
            final short[] colorRow = textBuffer.color[y];
            int cbg = 0x000000;
            int x = 0;
            int width = 0;
            for (int col = 0; col < colorRow.length && x + width < viewportWidth; col++) {
                int bg = PackedColor.unpackBackground(colorRow[col], format);
                if (bg != cbg) {
                    if (cbg != 0 && width > 0) {
                        drawBackgroundQuad(bgConsumer, localTransform, cbg, x, y, width, charWidth, charHeight, alpha);
                    }
                    cbg = bg;
                    x += width;
                    width = 0;
                }
                width++;
            }
            if (cbg != 0 && width > 0) {
                drawBackgroundQuad(bgConsumer, localTransform, cbg, x, y, width, charWidth, charHeight, alpha);
            }
        }

        if (buffers instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(BACKGROUND_TYPE);
        }

        if (renderer instanceof DynamicFontRenderer dfr) {
            renderForegroundDynamic(buffers, textBuffer, viewportWidth, viewportHeight,
                    localTransform, charWidth, charHeight, format, dfr, tintR, tintG, tintB, alpha);
        } else if (renderer instanceof StaticFontRenderer sfr) {
            renderForegroundStatic(buffers, textBuffer, viewportWidth, viewportHeight,
                    localTransform, charWidth, charHeight, format, sfr, tintR, tintG, tintB, alpha);
        }

        if (buffers instanceof MultiBufferSource.BufferSource bs) {
            bs.endLastBatch();
        }
    }

    private static void renderForegroundDynamic(MultiBufferSource buffers, TextBuffer textBuffer,
                                                int viewportWidth, int viewportHeight, Matrix4f localTransform,
                                                int charWidth, int charHeight, PackedColor.ColorFormat format,
                                                DynamicFontRenderer dfr, float tintR, float tintG, float tintB, float alpha) {
        for (int i = 0; i < dfr.textureCount(); i++) {
            final ResourceLocation loc = dfr.getFontTextureLocation(i);
            final VertexConsumer fgConsumer = buffers.getBuffer(TEXT_TYPE.apply(loc));

            for (int y = 0; y < Math.min(viewportHeight, textBuffer.height); y++) {
                final int[] line = textBuffer.buffer[y];
                final short[] colorRow = textBuffer.color[y];
                final float ty = y * charHeight;

                final int cols = Math.min(viewportWidth, line.length);
                for (int n = 0; n < cols; n++) {
                    final int ch = line[n];
                    if (ch == ' ') continue;
                    final int col = PackedColor.unpackForeground(colorRow[n], format);
                    final float r = ((col >> 16) & 0xFF) / 255f * tintR;
                    final float g = ((col >> 8) & 0xFF) / 255f * tintG;
                    final float b = (col & 0xFF) / 255f * tintB;

                    final TextureFontRenderer.CharRenderInfo info = dfr.getCharRenderInfo(ch);
                    if (info != null && info.textureIndex() == i) {
                        drawCharQuad(fgConsumer, localTransform, n * charWidth, ty, info, r, g, b, alpha);
                    }
                }
            }
        }
    }

    private static void renderForegroundStatic(MultiBufferSource buffers, TextBuffer textBuffer,
                                               int viewportWidth, int viewportHeight, Matrix4f localTransform,
                                               int charWidth, int charHeight, PackedColor.ColorFormat format,
                                               StaticFontRenderer sfr, float tintR, float tintG, float tintB, float alpha) {
        final ResourceLocation loc = sfr.getFontTextureLocation(0);
        final VertexConsumer fgConsumer = buffers.getBuffer(TEXT_TYPE.apply(loc));

        for (int y = 0; y < Math.min(viewportHeight, textBuffer.height); y++) {
            final int[] line = textBuffer.buffer[y];
            final short[] colorRow = textBuffer.color[y];
            final float ty = y * charHeight;

            final int cols = Math.min(viewportWidth, line.length);
            for (int n = 0; n < cols; n++) {
                final int ch = line[n];
                if (ch == ' ') continue;
                final int col = PackedColor.unpackForeground(colorRow[n], format);
                final float r = ((col >> 16) & 0xFF) / 255f * tintR;
                final float g = ((col >> 8) & 0xFF) / 255f * tintG;
                final float b = (col & 0xFF) / 255f * tintB;

                final TextureFontRenderer.CharRenderInfo info = sfr.getCharRenderInfo(ch);
                if (info != null) {
                    drawCharQuad(fgConsumer, localTransform, n * charWidth, ty, info, r, g, b, alpha);
                }
            }
        }
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

        final Vector4f p0 = new Vector4f(x0, y1, 0, 1.0f).mul(m); // bottom-left
        final Vector4f p1 = new Vector4f(x1, y1, 0, 1.0f).mul(m); // bottom-right
        final Vector4f p2 = new Vector4f(x1, y0, 0, 1.0f).mul(m); // top-right
        final Vector4f p3 = new Vector4f(x0, y0, 0, 1.0f).mul(m); // top-left

        float a = Math.clamp(alpha, 0.0f, 1.0f);
        consumer.addVertex(p0.x(), p0.y(), p0.z()).setColor(r, g, b, a);
        consumer.addVertex(p1.x(), p1.y(), p1.z()).setColor(r, g, b, a);
        consumer.addVertex(p2.x(), p2.y(), p2.z()).setColor(r, g, b, a);
        consumer.addVertex(p3.x(), p3.y(), p3.z()).setColor(r, g, b, a);
    }

    private static void drawCharQuad(VertexConsumer consumer, Matrix4f m, float tx, float ty,
                                     TextureFontRenderer.CharRenderInfo info, float r, float g, float b, float alpha) {
        final Vector4f p0 = new Vector4f(tx, ty + info.height(), 0, 1.0f).mul(m); // bottom-left
        final Vector4f p1 = new Vector4f(tx + info.width(), ty + info.height(), 0, 1.0f).mul(m); // bottom-right
        final Vector4f p2 = new Vector4f(tx + info.width(), ty, 0, 1.0f).mul(m); // top-right
        final Vector4f p3 = new Vector4f(tx, ty, 0, 1.0f).mul(m); // top-left

        float a = Math.clamp(alpha, 0.0f, 1.0f);
        consumer.addVertex(p0.x(), p0.y(), p0.z()).setColor(r, g, b, a).setUv(info.u1(), info.v2()).setLight(FULL_BRIGHT);
        consumer.addVertex(p1.x(), p1.y(), p1.z()).setColor(r, g, b, a).setUv(info.u2(), info.v2()).setLight(FULL_BRIGHT);
        consumer.addVertex(p2.x(), p2.y(), p2.z()).setColor(r, g, b, a).setUv(info.u2(), info.v1()).setLight(FULL_BRIGHT);
        consumer.addVertex(p3.x(), p3.y(), p3.z()).setColor(r, g, b, a).setUv(info.u1(), info.v1()).setLight(FULL_BRIGHT);
    }
}
