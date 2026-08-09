package li.cil.oc.core.impl.client.renderer.font;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public final class DynamicFontRenderer extends TextureFontRenderer {
    private static final int ATLAS_SIZE = 256;
    private static final String BASIC_CHARS = "☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■";

    private final FontParserHex glyphProvider = new FontParserHex();
    private final List<CharTexture> textures = new ArrayList<>();
    private final Map<Integer, CharIcon> charMap = new HashMap<>();

    public DynamicFontRenderer() {
        glyphProvider.initialize();
        initialize();
    }

    public void initialize() {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        for (CharTexture texture : textures) {
            tm.release(texture.location);
            texture.close();
        }
        textures.clear();
        charMap.clear();

        textures.add(new CharTexture(this));
        generateChars(BASIC_CHARS.codePoints().toArray());
    }

    @Override
    protected int charWidth() {
        return glyphProvider.getGlyphWidth();
    }

    @Override
    protected int charHeight() {
        return glyphProvider.getGlyphHeight();
    }

    @Override
    public int textureCount() {
        return textures.size();
    }

    @Override
    protected void generateChar(int charCode) {
        charMap.computeIfAbsent(charCode, this::createCharIcon);
    }

    public ResourceLocation getFontTextureLocation(int index) {
        return textures.get(index).location;
    }

    public CharRenderInfo getCharRenderInfo(int charCode) {
        CharIcon icon = charMap.get(charCode);
        if (icon == null) {
            icon = charMap.get((int) '?');
        }
        if (icon != null) {
            int idx = textures.indexOf(icon.texture);
            return new CharRenderInfo(idx, (float) icon.u1, (float) icon.v1, (float) icon.u2, (float) icon.v2, icon.w, icon.h);
        }
        return null;
    }

    private CharIcon createCharIcon(int charCode) {
        if (FontUtils.wcwidth(charCode) < 1 || glyphProvider.getGlyph(charCode) == null) {
            if (charCode == '?') {
                return null;
            }
            return charMap.computeIfAbsent((int) '?', this::createCharIcon);
        }

        if (textures.getLast().isFull(charCode)) {
            textures.add(new CharTexture(this));
        }

        return textures.getLast().add(charCode);
    }

    private ByteBuffer glyphData(int charCode) {
        return glyphProvider.getGlyph(charCode);
    }

    private static final AtomicInteger textureCounter = new AtomicInteger();

    private static final class CharTexture {
        private final DynamicFontRenderer owner;
        private final DynamicTexture texture;
        public final ResourceLocation location;
        private final int cellWidth;
        private final int cellHeight;
        private final int cols;
        private final double uStep;
        private final double vStep;
        private final double pad;
        private final int capacity;
        private int chars = 0;

        private CharTexture(DynamicFontRenderer owner) {
            this.owner = owner;
            this.texture = new DynamicTexture(ATLAS_SIZE, ATLAS_SIZE, false);
            this.location = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/font/dynamic_" + textureCounter.getAndIncrement());
            this.cellWidth = owner.charWidth() + 2;
            this.cellHeight = owner.charHeight() + 2;
            this.cols = ATLAS_SIZE / cellWidth;
            this.uStep = cellWidth / (double) ATLAS_SIZE;
            this.vStep = cellHeight / (double) ATLAS_SIZE;
            this.pad = 1.0 / ATLAS_SIZE;
            this.capacity = cols * (ATLAS_SIZE / cellHeight);

            final NativeImage image = texture.getPixels();
            if (image != null) {
                image.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE, 0x00000000);
                texture.upload();
            }
            Minecraft.getInstance().getTextureManager().register(location, texture);
            owner.applyTextLinearFilter(texture);
        }

        private void close() {
            try {
                texture.close();
            } catch (Exception ignored) {
            }
        }

        private boolean isFull(int charCode) {
            return chars + FontUtils.wcwidth(charCode) > capacity;
        }

        private CharIcon add(int charCode) {
            final int glyphWidth = FontUtils.wcwidth(charCode);
            final int w = owner.charWidth() * glyphWidth;
            final int h = owner.charHeight();

            if (chars % cols + glyphWidth > cols) {
                chars += 1;
            }

            final int x = chars % cols;
            final int y = chars / cols;

            final ByteBuffer glyph = owner.glyphData(charCode);
            final NativeImage image = texture.getPixels();
            if (glyph != null && image != null) {
                final int baseX = 1 + x * cellWidth;
                final int baseY = 1 + y * cellHeight;
                for (int py = 0; py < h; py++) {
                    final int rowOffset = py * w * 4;
                    for (int px = 0; px < w; px++) {
                        final int src = rowOffset + px * 4;
                        final int alpha = glyph.get(src) & 0xFF;
                        image.setPixelRGBA(baseX + px, baseY + py, alpha == 0 ? 0x00000000 : 0xFFFFFFFF);
                    }
                }
                texture.upload();
            }

            chars += glyphWidth;
            return new CharIcon(this, w, h, pad + x * uStep, pad + y * vStep, (x + glyphWidth) * uStep - pad, (y + 1) * vStep - pad);
        }
    }

    private record CharIcon(CharTexture texture, int w, int h, double u1, double v1, double u2, double v2) {
    }
}
