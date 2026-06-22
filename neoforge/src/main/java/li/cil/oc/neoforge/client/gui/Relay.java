package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;

public class Relay extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Relay> {
    public final li.cil.oc.neoforge.common.tileentity.Relay relay;
    private final DecimalFormat format = new DecimalFormat("#.##hz");

    public Relay(Inventory playerInventory, li.cil.oc.neoforge.common.tileentity.Relay relay) {
        super(new li.cil.oc.neoforge.common.container.Relay(0, playerInventory, relay, playerInventory.player));
        this.relay = relay;
    }

    public Relay(li.cil.oc.neoforge.common.container.Relay container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.relay = (li.cil.oc.neoforge.common.tileentity.Relay) container.otherInventory;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, dt, mouseX, mouseY);
        int x = windowX() + imageWidth;
        int y = windowY() + 10;
        int w = 23;
        int h = 26;
        guiGraphics.blit(Textures.guiUpgradeTab, x, y, (int) blitOffset, 0.0f, 0.0f, w, h, w, h);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int originalWidth = imageWidth;
        try {
            imageWidth += 23;
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            imageWidth = originalWidth;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int originalWidth = imageWidth;
        try {
            imageWidth += 23;
            return super.mouseReleased(mouseX, mouseY, button);
        } finally {
            imageWidth = originalWidth;
        }
    }

    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(relay.getInventoryName()).getString(), 8, 6, 0x404040);

        guiGraphics.drawString(font, Component.translatable("gui.opencomputers.switch.transferrate").getString(), 14, 20, 0x404040);
        guiGraphics.drawString(font, Component.translatable("gui.opencomputers.switch.packetspercycle").getString(), 14, 39, 0x404040);
        guiGraphics.drawString(font, Component.translatable("gui.opencomputers.switch.queuesize").getString(), 14, 58, 0x404040);
        guiGraphics.drawString(font, format.format(20f / menu.relayDelay()), 108, 20, 0x404040);
        guiGraphics.drawString(font, menu.packetsPerCycleAvg() + " / " + menu.relayAmount(), 108, 39,
                thresholdBasedColor(menu.packetsPerCycleAvg(), (int) Math.ceil(menu.relayAmount() / 2f), menu.relayAmount()));
        guiGraphics.drawString(font, menu.queueSize() + " / " + menu.maxQueueSize(), 108, 58,
                thresholdBasedColor(menu.queueSize(), menu.maxQueueSize() / 2, menu.maxQueueSize()));
    }

    private int thresholdBasedColor(int value, int yellow, int red) {
        if (value < yellow) return 0x009900;
        else if (value < red) return 0x999900;
        else return 0x990000;
    }
}
