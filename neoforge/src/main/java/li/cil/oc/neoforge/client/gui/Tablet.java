package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.common.item.TabletWrapper;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;

public class Tablet extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Tablet> implements li.cil.oc.core.impl.client.gui.traits.LockedHotbar {
    public final TabletWrapper tablet;
    private int lockedSlot = -1;

    @SuppressWarnings("unused")
    public Tablet(Inventory playerInventory, TabletWrapper tablet) {
        super(new li.cil.oc.core.impl.common.container.Tablet(Menus.TABLET.get(), 0, playerInventory, tablet));
        this.tablet = tablet;
    }

    public Tablet(li.cil.oc.core.impl.common.container.Tablet container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.tablet = (TabletWrapper) container.otherInventory;
    }

    @Override
    public int lockedSlot() {
        return lockedSlot;
    }

    @Override
    public void setLockedSlot(int s) {
        this.lockedSlot = s;
    }

    @Override
    public net.minecraft.world.item.ItemStack lockedStack() {
        return tablet.getStack();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        net.minecraft.world.inventory.Slot slot = null;
        for (net.minecraft.world.inventory.Slot s : getMenu().slots) {
            if (isHovering(s.x, s.y, 16, 16, x, y)) {
                slot = s;
                break;
            }
        }
        if (shouldSuppressClick(slot)) return true;
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected boolean checkHotbarKeyPressed(int ignoredKeyCode, int ignoredScanCode) {
        return false;
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, title, 8, 6, 0x404040, false);
    }
}
