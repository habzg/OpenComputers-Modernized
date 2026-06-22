package li.cil.oc.core.impl.client.renderer.font;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class FontParserHex {
    private static final byte[] OPAQUE = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] TRANSPARENT = {0, 0, 0, 0};
    private static final Logger LOGGER = LoggerFactory.getLogger(FontParserHex.class);

    private final Map<Integer, byte[]> glyphs = new HashMap<>();

    public void initialize() {
        glyphs.clear();

        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "font.hex");
        try {
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (resourceOpt.isEmpty()) {
                return;
            }

            try (InputStream font = resourceOpt.get().open();
                 BufferedReader input = new BufferedReader(new InputStreamReader(font, StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    int colonPos = line.indexOf(':');
                    if (colonPos <= 0) continue;
                    String info = line.substring(0, colonPos);
                    int charCode;
                    try {
                        charCode = Integer.parseInt(info, 16);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    if (charCode < 0 || charCode >= FontUtils.codepoint_limit) continue;
                    int expectedWidth = FontUtils.wcwidth(charCode);
                    if (expectedWidth < 1) continue;

                    int glyphStrOfs = info.length() + 1;
                    int glyphLen = (line.length() - glyphStrOfs) >> 1;
                    if (glyphLen <= 0) continue;
                    byte[] glyph = new byte[glyphLen];
                    int glyphWidth = glyph.length / getGlyphHeight();
                    if (expectedWidth != glyphWidth) {
                        if (Settings.get().logHexFontErrors) {
                            LOGGER.warn("Size of glyph for code point U+{} ({}) in font ({}) does not match expected width ({}), ignoring.",
                                    String.format("%04X", charCode), (char) charCode, glyphWidth, expectedWidth);
                        }
                        continue;
                    }
                    for (int i = 0; i < glyph.length; i++, glyphStrOfs += 2) {
                        glyph[i] = (byte) ((hex2int(line.charAt(glyphStrOfs)) << 4) | hex2int(line.charAt(glyphStrOfs + 1)));
                    }
                    glyphs.put(charCode, glyph);
                }
            }
        } catch (IOException e) {
            if (Settings.get().logHexFontErrors) {
                LOGGER.warn("Failed loading glyphs.", e);
            }
        }
    }

    public ByteBuffer getGlyph(int charCode) {
        byte[] glyph = glyphs.get(charCode);
        if (glyph == null || glyph.length == 0) return null;

        ByteBuffer buffer = ByteBuffer.allocateDirect(glyph.length * getGlyphWidth() * 4);
        for (byte aGlyph : glyph) {
            int c = aGlyph & 0xFF;
            for (int j = 0; j < 8; j++) {
                buffer.put((c & 0x80) != 0 ? OPAQUE : TRANSPARENT);
                c <<= 1;
            }
        }
        buffer.flip();
        return buffer;
    }

    @SuppressWarnings("SameReturnValue")
    public int getGlyphWidth() {
        return 8;
    }

    @SuppressWarnings("SameReturnValue")
    public int getGlyphHeight() {
        return 16;
    }

    private static int hex2int(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - ('A' - 10);
        if (c >= 'a' && c <= 'f') return c - ('a' - 10);
        throw new IllegalArgumentException("invalid char: " + c);
    }
}
