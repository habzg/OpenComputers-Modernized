package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Case extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Case> {
    public final li.cil.oc.core.impl.common.tileentity.Case computer;
    private ImageButton powerButton;

    @SuppressWarnings("unused")
    public Case(Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Case computer) {
        super(new li.cil.oc.neoforge.common.container.Case(Menus.CASE.get(), 0, playerInventory, computer));
        this.computer = computer;
    }

    public Case(li.cil.oc.neoforge.common.container.Case container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.computer = (li.cil.oc.core.impl.common.tileentity.Case) container.otherInventory;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled = computer.isRunning();
        super.render(guiGraphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        powerButton = new ImageButton(0, leftPos + 70, topPos + 33, 18, 18, Textures.guiButtonPower, true);
        addRenderableWidget(powerButton);
        powerButton.setPressHandler(this::actionPerformed);
    }

    protected void actionPerformed(AbstractWidget button) {
        if (button == powerButton) {
            PacketSender.sendComputerPower(computer, !computer.isRunning());
        }
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(computer.getInventoryName()).getString(), 8, 6, 0x404040);
        if (powerButton.isHoveredOrFocused()) {
            List<Component> tooltip = new ArrayList<>();
            for (String line : (computer.isRunning() ?
                    Component.translatable("gui.opencomputers.robot.turnoff").getString() : Component.translatable("gui.opencomputers.robot.turnon").getString()).split("\n")) {
                tooltip.add(Component.literal(line));
            }
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics guiGraphics) {
        guiGraphics.blit(Textures.guiComputer, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
