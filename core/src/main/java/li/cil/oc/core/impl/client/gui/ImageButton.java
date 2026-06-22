package li.cil.oc.core.impl.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ImageButton extends AbstractWidget {
    private final int id;
    public final ResourceLocation image;
    public final boolean canToggle;
    public final int textColor;
    public final int textDisabledColor;
    public final int textHoverColor;
    public final int textIndent;
    public boolean toggled = false;
    public boolean hoverOverride = false;
    private Consumer<ImageButton> pressHandler;

    public ImageButton(int id, int x, int y, int w, int h, ResourceLocation image) {
        this(id, x, y, w, h, image, null, 0xE0E0E0, false);
    }

    public ImageButton(int id, int x, int y, int w, int h, ResourceLocation image, boolean canToggle) {
        this(id, x, y, w, h, image, null, 0xE0E0E0, canToggle);
    }

    @SuppressWarnings("unused")
    public ImageButton(int id, int x, int y, int w, int h, ResourceLocation image, String text) {
        this(id, x, y, w, h, image, text, 0xE0E0E0, false);
    }

    public ImageButton(int id, int x, int y, int w, int h, ResourceLocation image, String text, int textColor, boolean canToggle) {
        this(id, x, y, w, h, image, text, textColor, canToggle, 0xA0A0A0, 0xFFFFA0, -1);
    }

    public ImageButton(int id, int x, int y, int w, int h, ResourceLocation image, String text, int textColor, boolean canToggle, int textDisabledColor, int textHoverColor, int textIndent) {
        super(x, y, w, h, Component.literal(text != null ? text : ""));
        this.id = id;
        this.image = image;
        this.canToggle = canToggle;
        this.textColor = textColor;
        this.textDisabledColor = textDisabledColor;
        this.textHoverColor = textHoverColor;
        this.textIndent = textIndent;
    }

    public int getId() {
        return id;
    }

    public void setPressHandler(Consumer<ImageButton> handler) {
        this.pressHandler = handler;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (pressHandler != null) {
            pressHandler.accept(this);
        }
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput narration) {
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        if (visible) {
            this.isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
            int x0 = getX();
            int x1 = getX() + width;
            int y0 = getY();
            int y1 = getY() + height;
            boolean hovered = hoverOverride || isHovered;
            if (image != null) {
                int texWidth = canToggle ? width * 2 : width;
                int texHeight = height * 2;
                int u = toggled && canToggle ? width : 0;
                int v = hovered ? height : 0;
                guiGraphics.blit(image, x0, y0, width, height, u, v, width, height, texWidth, texHeight);
            } else if (hovered) {
                guiGraphics.fill(x0, y0, x1, y1, 0xCCFFFFFF);
            } else {
                guiGraphics.fill(x0, y0, x1, y1, 0x66FFFFFF);
            }
            if (!getMessage().getString().isEmpty()) {
                int color = !active ? textDisabledColor :
                        (hoverOverride || isHovered) ? textHoverColor : textColor;
                if (textIndent >= 0)
                    guiGraphics.drawString(Minecraft.getInstance().font, getMessage(), textIndent + getX(), getY() + (height - 8) / 2, color);
                else
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
            }
        }
    }
}
