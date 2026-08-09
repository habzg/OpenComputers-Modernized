package li.cil.oc.neoforge.client.gui;

import java.util.ArrayList;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import li.cil.oc.core.impl.common.container.ComponentSlot;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class Printer extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Printer> {
    public final li.cil.oc.core.impl.common.blockentity.Printer printer;
    private final ProgressBar materialBar;
    private final ProgressBar inkBar;
    private final ProgressBar progressBar;

    @SuppressWarnings("unused")
    public Printer(Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Printer printer) {
        super(new li.cil.oc.core.impl.common.container.Printer(Menus.PRINTER.get(), 0, playerInventory, printer, playerInventory.player));
        this.printer = printer;
        imageWidth = 176;
        imageHeight = 166;
        materialBar = addWidget(new ProgressBar(40, 21) {
            @Override
            public int width() {
                return 62;
            }

            @Override
            public ResourceLocation barTexture() {
                return Textures.guiPrinterMaterial;
            }
        });
        inkBar = addWidget(new ProgressBar(40, 53) {
            @Override
            public int width() {
                return 62;
            }

            @Override
            public ResourceLocation barTexture() {
                return Textures.guiPrinterInk;
            }
        });
        progressBar = addWidget(new ProgressBar(105, 20) {
            @Override
            public int width() {
                return 46;
            }

            @Override
            public int height() {
                return 46;
            }

            @Override
            public ResourceLocation barTexture() {
                return Textures.guiPrinterProgress;
            }
        });
    }

    public Printer(li.cil.oc.core.impl.common.container.Printer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.printer = (li.cil.oc.core.impl.common.blockentity.Printer) container.otherInventory;
        imageWidth = 176;
        imageHeight = 166;
        materialBar = addWidget(new ProgressBar(40, 21) {
            @SuppressWarnings("SameReturnValue")
            @Override
            public int width() {
                return 62;
            }

            @SuppressWarnings("SameReturnValue")
            @Override
            public ResourceLocation barTexture() {
                return Textures.guiPrinterMaterial;
            }
        });
        inkBar = addWidget(new ProgressBar(40, 53) {
            @SuppressWarnings("SameReturnValue")
            @Override
            public int width() {
                return 62;
            }

            @SuppressWarnings("SameReturnValue")
            @Override
            public ResourceLocation barTexture() {
                return Textures.guiPrinterInk;
            }
        });
        progressBar = addWidget(new ProgressBar(105, 20) {
            @SuppressWarnings("SameReturnValue")
            @Override
            public int width() {
                return 46;
            }

            @SuppressWarnings("SameReturnValue")
            @Override
            public int height() {
                return 46;
            }

            @SuppressWarnings("SameReturnValue")
            @Override
            public ResourceLocation barTexture() {
                return Textures.guiPrinterProgress;
            }
        });
    }

    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable(printer.getInventoryName()).getString(), 8, 6, 0x404040, false);
        if (isHovering(materialBar.x, materialBar.y, materialBar.width(), materialBar.height(), mouseX, mouseY)) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(Component.literal(menu.amountMaterial() + "/" + li.cil.oc.core.impl.common.blockentity.Printer.MAX_AMOUNT_MATERIAL));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
        if (isHovering(inkBar.x, inkBar.y, inkBar.width(), inkBar.height(), mouseX, mouseY)) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(Component.literal(menu.amountInk() + "/" + li.cil.oc.core.impl.common.blockentity.Printer.MAX_AMOUNT_INK));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float ignoredDt, int ignoredMouseX, int ignoredMouseY) {
        guiGraphics.blit(Textures.guiPrinter, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        materialBar.level = (double) menu.amountMaterial() / li.cil.oc.core.impl.common.blockentity.Printer.MAX_AMOUNT_MATERIAL;
        inkBar.level = (double) menu.amountInk() / li.cil.oc.core.impl.common.blockentity.Printer.MAX_AMOUNT_INK;
        progressBar.level = menu.progress();
        drawWidgets(guiGraphics);
    }

    @Override
    protected void drawDisabledSlot(GuiGraphics ignoredGuiGraphics, ComponentSlot ignoredSlot) {
    }

}
