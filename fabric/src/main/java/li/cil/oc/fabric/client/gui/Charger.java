package li.cil.oc.fabric.client.gui;

import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Charger extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Charger> {
    public final li.cil.oc.core.impl.common.blockentity.Charger charger;

    @SuppressWarnings("unused")
    public Charger(Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Charger charger) {
        super(new li.cil.oc.core.impl.common.container.Charger(Menus.CHARGER, 0, playerInventory, charger));
        this.charger = charger;
    }

    public Charger(li.cil.oc.core.impl.common.container.Charger container, Inventory inv, Component title) {
        super(container, inv, title);
        this.charger = (li.cil.oc.core.impl.common.blockentity.Charger) container.otherInventory;
    }

    @SuppressWarnings("unused")
    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(charger.getInventoryName()).getString(), 8, 6, 0x404040, false);
    }
}
