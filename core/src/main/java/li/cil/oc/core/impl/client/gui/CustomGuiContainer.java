package li.cil.oc.core.impl.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.core.impl.client.gui.widget.WidgetContainer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

public abstract class CustomGuiContainer<C extends AbstractContainerMenu> extends AbstractContainerScreen<C> implements WidgetContainer {
    protected float blitOffset;

    @SuppressWarnings({"unused", "DataFlowIssue"})
    public CustomGuiContainer(C inventoryContainer) {
        super(inventoryContainer, null, null);
    }

    @SuppressWarnings("unused")
    public CustomGuiContainer(C inventoryContainer, Inventory inv, Component title) {
        super(inventoryContainer, inv, title);
    }

    public int windowX() {
        return leftPos;
    }

    public int windowY() {
        return topPos;
    }

    @SuppressWarnings("unused")
    public float windowZ() {
        return blitOffset;
    }

    protected void renderTooltip(GuiGraphics guiGraphics, List<String> text, int x, int y, Font font) {
        copiedRenderTooltip(guiGraphics, text, x, y, font);
    }

    protected void copiedRenderTooltip(GuiGraphics guiGraphics, List<String> text, int x, int y, Font font) {
        if (!text.isEmpty()) {
            RenderSystem.disableDepthTest();
            int textWidth = 0;
            for (String line : text) {
                int w = font.width(line);
                if (w > textWidth) textWidth = w;
            }
            int posX = x + 12;
            int posY = y - 12;
            int textHeight = 8;
            if (text.size() > 1) {
                textHeight += 2 + (text.size() - 1) * 10;
            }
            if (posX + textWidth > this.width) {
                posX -= 28 + textWidth;
            }
            if (posY + textHeight + 6 > this.height) {
                posY = this.height - textHeight - 6;
            }
            float oldZ = blitOffset;
            blitOffset = 300f;
            int bg = 0xF0100010;
            guiGraphics.fillGradient(posX - 3, posY - 4, posX + textWidth + 3, posY - 3, bg, bg);
            guiGraphics.fillGradient(posX - 3, posY + textHeight + 3, posX + textWidth + 3, posY + textHeight + 4, bg, bg);
            guiGraphics.fillGradient(posX - 3, posY - 3, posX + textWidth + 3, posY + textHeight + 3, bg, bg);
            guiGraphics.fillGradient(posX - 4, posY - 3, posX - 3, posY + textHeight + 3, bg, bg);
            guiGraphics.fillGradient(posX + textWidth + 3, posY - 3, posX + textWidth + 4, posY + textHeight + 3, bg, bg);
            int color1 = 0x505000FF;
            int color2 = 0x505000FE;
            guiGraphics.fillGradient(posX - 3, posY - 3 + 1, posX - 3 + 1, posY + textHeight + 3 - 1, color1, color2);
            guiGraphics.fillGradient(posX + textWidth + 2, posY - 3 + 1, posX + textWidth + 3, posY + textHeight + 3 - 1, color1, color2);
            guiGraphics.fillGradient(posX - 3, posY - 3, posX + textWidth + 3, posY - 3 + 1, color1, color1);
            guiGraphics.fillGradient(posX - 3, posY + textHeight + 2, posX + textWidth + 3, posY + textHeight + 3, color2, color2);
            for (int i = 0; i < text.size(); i++) {
                String line = text.get(i);
                guiGraphics.drawString(font, line, posX, posY, -1);
                if (i == 0) posY += 2;
                posY += 10;
            }
            blitOffset = oldZ;
            RenderSystem.enableDepthTest();
        }
    }
}
