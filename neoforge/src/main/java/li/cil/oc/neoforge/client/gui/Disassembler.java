package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Disassembler extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Disassembler> {
    public final li.cil.oc.core.impl.common.blockentity.Disassembler disassembler;
    public final ProgressBar progress;

    @SuppressWarnings("unused")
    public Disassembler(Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Disassembler disassembler) {
        super(new li.cil.oc.core.impl.common.container.Disassembler(Menus.DISASSEMBLER.get(), 0, playerInventory, disassembler, playerInventory.player));
        this.disassembler = disassembler;
        progress = addWidget(new ProgressBar(18, 65));
    }

    public Disassembler(li.cil.oc.core.impl.common.container.Disassembler container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.disassembler = (li.cil.oc.core.impl.common.blockentity.Disassembler) container.otherInventory;
        progress = addWidget(new ProgressBar(18, 65));
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int ignoredMouseX, int ignoredMouseY) {
        guiGraphics.drawString(font, Component.translatable(disassembler.getInventoryName()).getString(), 8, 6, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float ignoredDt, int ignoredMouseX, int ignoredMouseY) {
        guiGraphics.blit(Textures.guiDisassembler, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        progress.level = menu.disassemblyProgress() / 100.0;
        drawWidgets(guiGraphics);
    }
}
