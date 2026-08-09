package li.cil.oc.core.impl.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.container.ComponentSlot;
import li.cil.oc.core.impl.common.container.Player;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public abstract class DynamicGuiContainer<C extends AbstractContainerMenu> extends CustomGuiContainer<C> {
    private Slot hoveredSlotOptional = null;

    @SuppressWarnings("unused")
    public DynamicGuiContainer(C container) {
        super(container);
    }

    @SuppressWarnings("unused")
    public DynamicGuiContainer(C container, net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
    }

    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("container.inventory").getString(), 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        for (Slot slot : menu.slots) {
            drawSlotHighlight(guiGraphics, slot);
        }
    }

    protected void drawSecondaryBackgroundLayer(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        guiGraphics.blit(Textures.guiBackground, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        drawSecondaryBackgroundLayer(guiGraphics);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        Slot slot = menu.slots.stream()
                .filter(s -> isHovering(s.x, s.y, 16, 16, mouseX, mouseY))
                .findFirst().orElse(null);
        hoveredSlotOptional = slot;
        this.hoveredSlot = slot;
        super.render(guiGraphics, mouseX, mouseY, dt);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderSlot(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot) {
        if (slot instanceof ComponentSlot cs) {
            if (cs.slot().equals(li.cil.oc.core.common.Slot.None) || cs.tier() == Tier.None) {
                if (!slot.hasItem() && cs.tierIcon() != null) {
                    drawDisabledSlot(guiGraphics, cs);
                }
                return;
            }
        }
        blitOffset += 1;
        if (!isInPlayerInventory(slot)) {
            drawSlotBackground(guiGraphics, slot.x, slot.y);
        }
        if (!slot.hasItem() && slot instanceof ComponentSlot cs) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (cs.tierIcon() != null) {
                guiGraphics.blit(cs.tierIcon(), slot.x, slot.y, (int) blitOffset, 0, 0, 16, 16, 16, 16);
            }
            var slotIcon = Icons.get(cs.slot());
            if (slotIcon != null) {
                guiGraphics.blit(slotIcon, slot.x, slot.y, (int) blitOffset, 0, 0, 16, 16, 16, 16);
            }
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        blitOffset -= 1;
        super.renderSlot(guiGraphics, slot);
    }

    protected void drawSlotHighlight(GuiGraphics guiGraphics, Slot slot) {
        if (getMenu().getCarried().isEmpty()) {
            if (slot instanceof ComponentSlot cs) {
                if (cs.slot().equals(li.cil.oc.core.common.Slot.None) || cs.tier() == Tier.None) return;
            }
            boolean currentIsInPlayerInventory = isInPlayerInventory(slot);
            boolean drawHighlight = hoveredSlotOptional != null && currentIsInPlayerInventory != isInPlayerInventory(hoveredSlotOptional) &&
                    ((currentIsInPlayerInventory && slot.hasItem() && isSelectiveSlot(hoveredSlotOptional) && hoveredSlotOptional.mayPlace(slot.getItem())) ||
                            (!currentIsInPlayerInventory && hoveredSlotOptional.hasItem() && isSelectiveSlot(slot) && slot.mayPlace(hoveredSlotOptional.getItem())));
            if (drawHighlight) {
                blitOffset += 100;
                guiGraphics.fillGradient(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF, 0x80FFFFFF);
                blitOffset -= 100;
            }
        }
    }

    private boolean isSelectiveSlot(Slot slot) {
        if (slot instanceof ComponentSlot cs) {
            return !cs.slot().equals(li.cil.oc.core.common.Slot.Any) && !cs.slot().equals(li.cil.oc.core.common.Slot.Tool);
        }
        return false;
    }

    protected void drawDisabledSlot(GuiGraphics guiGraphics, ComponentSlot slot) {
        var sprite = slot.tierIcon();
        guiGraphics.blit(sprite, slot.x, slot.y, (int) blitOffset, 0, 0, 16, 16, 16, 16);
    }

    protected void drawSlotBackground(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(Textures.guiSlot, x - 1, y - 1, (int) blitOffset, 0, 0, 18, 18, 18, 18);
    }

    private boolean isInPlayerInventory(Slot slot) {
        if (menu instanceof Player) {
            return slot.container == ((Player) menu).playerInventory;
        }
        return false;
    }
}
