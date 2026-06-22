package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Disassembler extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Disassembler> {
    public final li.cil.oc.core.impl.common.tileentity.Disassembler disassembler;
    public final ProgressBar progress;

    @SuppressWarnings("unused")
    public Disassembler(Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Disassembler disassembler) {
        super(new li.cil.oc.neoforge.common.container.Disassembler(0, playerInventory, disassembler, playerInventory.player));
        this.disassembler = disassembler;
        progress = addWidget(new ProgressBar(18, 65));
    }

    public Disassembler(li.cil.oc.neoforge.common.container.Disassembler container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.disassembler = (li.cil.oc.core.impl.common.tileentity.Disassembler) container.otherInventory;
        progress = addWidget(new ProgressBar(18, 65));
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable(disassembler.getInventoryName()).getString(), 8, 6, 0x404040);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        guiGraphics.blit(Textures.guiDisassembler, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        progress.level = menu.disassemblyProgress() / 100.0;
        drawWidgets(guiGraphics);
    }
}
