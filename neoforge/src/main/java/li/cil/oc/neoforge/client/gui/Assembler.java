package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import li.cil.oc.core.impl.common.template.AssemblerTemplates;
import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.common.container.ComponentSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Assembler extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Assembler> {
    public final li.cil.oc.core.impl.common.tileentity.Assembler assembler;
    private final ProgressBar progress;
    private AssemblerTemplates.ValidationResult info = null;
    private ImageButton runButton;

    @SuppressWarnings("unused")
    public Assembler(Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Assembler assembler) {
        super(new li.cil.oc.neoforge.common.container.Assembler(0, playerInventory, assembler, playerInventory.player));
        this.assembler = assembler;
        imageWidth = 176;
        imageHeight = 192;
        for (Slot slot : menu.slots) {
            if (slot instanceof ComponentSlot) {
                ((ComponentSlot) slot).changeListener = this::onSlotChanged;
            }
        }
        progress = addWidget(new ProgressBar(28, 92));
    }

    public Assembler(li.cil.oc.neoforge.common.container.Assembler container, Inventory inv, Component title) {
        super(container, inv, title);
        this.assembler = (li.cil.oc.core.impl.common.tileentity.Assembler) container.otherInventory;
        imageWidth = 176;
        imageHeight = 192;
        for (Slot slot : menu.slots) {
            if (slot instanceof ComponentSlot) {
                ((ComponentSlot) slot).changeListener = this::onSlotChanged;
            }
        }
        progress = addWidget(new ProgressBar(28, 92));
    }

    @SuppressWarnings("unused")
    private void onSlotChanged(Slot slot) {
        runButton.active = canBuild();
        runButton.toggled = !runButton.active;
        info = validate();
    }

    private AssemblerTemplates.ValidationResult validate() {
        var template = AssemblerTemplates.select(menu.getSlot(0).getItem());
        if (template != null) {
            return template.validate(menu.otherInventory);
        }
        return null;
    }

    private boolean canBuild() {
        var val = validate();
        return !menu.isAssembling() && val != null && val.valid();
    }

    protected void actionPerformed(AbstractWidget button) {
        if (button == runButton && canBuild()) {
            PacketSender.sendRobotAssemblerStart(assembler);
        }
    }

    @Override
    public void init() {
        super.init();
        runButton = new ImageButton(0, leftPos + 7, topPos + 89, 18, 18, Textures.guiButtonRun, true);
        addRenderableWidget(runButton);
        runButton.setPressHandler(this::actionPerformed);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!menu.isAssembling()) {
            String message;
            if (!menu.getSlot(0).hasItem()) {
                message = Component.translatable("gui.opencomputers.assembler.insertcase").getString();
            } else {
                message = info != null ? (info.value() != null ? info.value().getString() : null) : (menu.getSlot(0).hasItem() ? Component.translatable("gui.opencomputers.assembler.collect").getString() : "");
            }
            guiGraphics.drawString(font, message, 30, 94, 0x404040);
            if (runButton.isHoveredOrFocused()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.opencomputers.assembler.run"));
                if (info != null && info.valid()) {
                    Collections.addAll(tooltip, info.warnings());
                }
                guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
            }
        } else if (isHovering(progress.x, progress.y, progress.width(), progress.height(), mouseX, mouseY)) {
            java.util.List<Component> tooltip = new ArrayList<>();
            String timeRemaining = formatTime(menu.assemblyRemainingTime());
            tooltip.add(Component.translatable("gui.opencomputers.assembler.progress", (int) menu.assemblyProgress(), timeRemaining));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60) return String.format("0:%02d", seconds);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        guiGraphics.blit(Textures.guiRobotAssembler, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.isAssembling()) progress.level = menu.assemblyProgress() / 100.0;
        else progress.level = 0;
        drawWidgets(guiGraphics);
    }

    @Override
    protected void drawDisabledSlot(GuiGraphics guiGraphics, ComponentSlot slot) {
    }

}
