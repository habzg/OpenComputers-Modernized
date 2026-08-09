package li.cil.oc.fabric.client.gui;

import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DiskDrive extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.DiskDrive> {

    @SuppressWarnings("unused")
    public DiskDrive(Inventory playerInventory, net.minecraft.world.Container drive) {
        super(new li.cil.oc.core.impl.common.container.DiskDrive(Menus.DISK_DRIVE, 0, playerInventory, drive));
    }

    public DiskDrive(li.cil.oc.core.impl.common.container.DiskDrive container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @SuppressWarnings("unused")
    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, title.getString(), 8, 6, 0x404040, false);
    }
}
