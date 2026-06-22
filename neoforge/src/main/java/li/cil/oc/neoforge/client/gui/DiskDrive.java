package li.cil.oc.neoforge.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DiskDrive extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.DiskDrive> {

    @SuppressWarnings("unused")
    public DiskDrive(Inventory playerInventory, net.minecraft.world.Container drive) {
        super(new li.cil.oc.neoforge.common.container.DiskDrive(0, playerInventory, drive));
    }

    public DiskDrive(li.cil.oc.neoforge.common.container.DiskDrive container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, title.getString(), 8, 6, 0x404040);
    }
}
