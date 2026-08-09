package li.cil.oc.fabric.client.gui;

import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Adapter extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Adapter> {
    public final li.cil.oc.core.impl.common.blockentity.Adapter adapter;

    @SuppressWarnings("unused")
    public Adapter(Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Adapter adapter) {
        super(new li.cil.oc.core.impl.common.container.Adapter(Menus.ADAPTER, 0, playerInventory, adapter));
        this.adapter = adapter;
    }

    public Adapter(li.cil.oc.core.impl.common.container.Adapter container, Inventory inv, Component title) {
        super(container, inv, title);
        this.adapter = (li.cil.oc.core.impl.common.blockentity.Adapter) container.otherInventory;
    }

    @SuppressWarnings("unused")
    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(adapter.getInventoryName()).getString(), 8, 6, 0x404040, false);
    }
}
