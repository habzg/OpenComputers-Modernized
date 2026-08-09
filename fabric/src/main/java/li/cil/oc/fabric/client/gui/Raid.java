package li.cil.oc.fabric.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Raid extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Raid> {
    public final li.cil.oc.core.impl.common.blockentity.Raid raid;

    @SuppressWarnings("unused")
    public Raid(Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Raid raid) {
        super(new li.cil.oc.core.impl.common.container.Raid(Menus.RAID, 0, playerInventory, raid));
        this.raid = raid;
    }

    public Raid(li.cil.oc.core.impl.common.container.Raid container, Inventory inv, Component title) {
        super(container, inv, title);
        this.raid = (li.cil.oc.core.impl.common.blockentity.Raid) container.otherInventory;
    }

    @SuppressWarnings("unused")
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(raid.getInventoryName()).getString(), 8, 6, 0x404040, false);
        var lines = font.split(Component.literal(Component.translatable("gui.opencomputers.raid.warning").getString()), width - 16);
        int y = 46;
        for (var line : lines) {
            guiGraphics.drawString(font, line, 8, y, 0x404040, false);
            y += font.lineHeight;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        guiGraphics.blit(Textures.guiRaid, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
