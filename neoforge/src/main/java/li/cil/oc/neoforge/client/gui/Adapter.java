package li.cil.oc.neoforge.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Adapter extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Adapter> {
    public final li.cil.oc.core.impl.common.tileentity.Adapter adapter;

    @SuppressWarnings("unused")
    public Adapter(Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Adapter adapter) {
        super(new li.cil.oc.neoforge.common.container.Adapter(0, playerInventory, adapter));
        this.adapter = adapter;
    }

    public Adapter(li.cil.oc.neoforge.common.container.Adapter container, Inventory inv, Component title) {
        super(container, inv, title);
        this.adapter = (li.cil.oc.core.impl.common.tileentity.Adapter) container.otherInventory;
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(adapter.getInventoryName()).getString(), 8, 6, 0x404040);
    }
}
