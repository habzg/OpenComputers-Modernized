package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.neoforge.common.inventory.DatabaseInventory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;

public class Database extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Database> implements li.cil.oc.core.impl.client.gui.traits.LockedHotbar {
    public final DatabaseInventory databaseInventory;
    private int lockedSlot = -1;

    public Database(Inventory playerInventory, DatabaseInventory databaseInventory) {
        super(new li.cil.oc.neoforge.common.container.Database(0, playerInventory, databaseInventory));
        this.databaseInventory = databaseInventory;
        imageHeight = 256;
    }

    public Database(li.cil.oc.neoforge.common.container.Database container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.databaseInventory = (DatabaseInventory) container.otherInventory;
        imageHeight = 256;
    }

    @Override
    public int lockedSlot() {
        return lockedSlot;
    }

    @Override
    public void setLockedSlot(int slot) {
        this.lockedSlot = slot;
    }

    @Override
    public net.minecraft.world.item.ItemStack lockedStack() {
        return databaseInventory.container();
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
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        return false;
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        guiGraphics.blit(Textures.guiDatabase, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (databaseInventory.tier() > Tier.One) {
            guiGraphics.blit(Textures.guiDatabase1, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        }
        if (databaseInventory.tier() > Tier.Two) {
            guiGraphics.blit(Textures.guiDatabase2, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        }
    }
}
