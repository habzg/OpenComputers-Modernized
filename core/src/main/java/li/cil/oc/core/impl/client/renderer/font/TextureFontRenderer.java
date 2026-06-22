package li.cil.oc.core.impl.client.renderer.font;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.core.impl.Settings;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public abstract class TextureFontRenderer implements IFontRenderer {
    public record CharRenderInfo(int textureIndex, float u1, float v1, float u2, float v2, int width, int height) {
    }

    @Override
    public int charRenderWidth() {
        return charWidth() / 2;
    }

    @Override
    public int charRenderHeight() {
        return charHeight() / 2;
    }

    @Override
    public void generateChars(int[] chars) {
        for (int ch : chars) {
            generateChar(ch);
        }
    }

    protected abstract int charWidth();

    protected abstract int charHeight();

    @SuppressWarnings("unused")
    public abstract int textureCount();

    @SuppressWarnings("unused")
    public abstract ResourceLocation getFontTextureLocation(int index);

    @SuppressWarnings("unused")
    public abstract CharRenderInfo getCharRenderInfo(int charCode);

    protected abstract void generateChar(int charCode);

    protected void applyTextLinearFilter(AbstractTexture texture) {
        texture.bind();
        int filter = Settings.get().textLinearFiltering ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }
}
