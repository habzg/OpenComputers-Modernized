package li.cil.oc.core.impl.client.gui.widget;

import li.cil.oc.core.impl.client.Textures;
import net.minecraft.resources.ResourceLocation;

public class ProgressBar extends Widget {
    public final int x;
    public final int y;
    public double level = 0.0;

    @SuppressWarnings("unused")
    public ProgressBar(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int width() {
        return 140;
    }

    @Override
    public int height() {
        return 12;
    }

    public ResourceLocation barTexture() {
        return Textures.guiBar;
    }

    @Override
    public void draw() {
        if (level > 0 && guiGraphics != null) {
            int tx = owner.windowX() + x;
            int ty = owner.windowY() + y;
            int w = (int) (width() * level);
            guiGraphics.blit(barTexture(), tx, ty, 0, 0, w, height(), width(), height());
        }
    }
}
