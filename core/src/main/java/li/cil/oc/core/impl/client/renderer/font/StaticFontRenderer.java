package li.cil.oc.core.impl.client.renderer.font;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public class StaticFontRenderer extends TextureFontRenderer {

    private final String chars;
    private final int charWidth;
    private final int charHeight;
    private final int cols;
    private final double uStep;
    private final double uSize;
    private final double vStep;
    private final double vSize;
    private final double s;

    public StaticFontRenderer() {
        int cw = 10, ch = 18;
        String charsStr = "☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■";
        try {
            var resourceOpt = Minecraft.getInstance().getResourceManager()
                    .getResource(ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/font/chars.txt"));
            if (resourceOpt.isEmpty()) throw new RuntimeException("chars.txt resource not found");
            InputStream is = resourceOpt.get().open();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            charsStr = reader.readLine();
            String sizeLine = reader.readLine();
            if (sizeLine != null) {
                String[] parts = sizeLine.split(" ", 2);
                cw = Integer.parseInt(parts[0]);
                ch = Integer.parseInt(parts[1]);
            }
            reader.close();
        } catch (Exception ignored) {
        }
        this.chars = charsStr;
        this.charWidth = cw;
        this.charHeight = ch;
        this.cols = 256 / charWidth;
        this.uStep = charWidth / 256.0;
        this.uSize = uStep;
        this.vStep = (charHeight + 1) / 256.0;
        this.vSize = charHeight / 256.0;
        this.s = OCSettings.get().fontCharScale;
    }

    @Override
    protected int charWidth() {
        return charWidth;
    }

    @Override
    protected int charHeight() {
        return charHeight;
    }

    @Override
    public int textureCount() {
        return 1;
    }

    private ResourceLocation lastFiltered = null;

    public ResourceLocation getFontTextureLocation(int index) {
        ResourceLocation loc = OCSettings.get().textAntiAlias ? Textures.fontAntiAliased : Textures.fontAliased;
        if (loc != lastFiltered) {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(loc);
            applyTextLinearFilter(texture);
            lastFiltered = loc;
        }
        return loc;
    }

    public CharRenderInfo getCharRenderInfo(int charCode) {
        int index = 1 + chars.indexOf(charCode);
        if (index == 0) {
            index = 1 + chars.indexOf('?');
            if (index == 0) return null;
        }
        int cx = (index - 1) % cols;
        int cy = (index - 1) / cols;
        float u = (float) (cx * uStep);
        float v = (float) (cy * vStep);
        return new CharRenderInfo(0, u, v, u + (float) uSize, v + (float) vSize, (int) (charWidth * s), (int) (charHeight * s));
    }

    @Override
    protected void generateChar(int charCode) {
    }
}
