package li.cil.oc.fabric.client.gui;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.client.gui.traits.LockedHotbar;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.fabric.client.PacketSender;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Server extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Server> implements LockedHotbar {
    public final ServerInventory serverInventory;
    private Rack rack;
    private int slot;
    private boolean resolvedRackFromContainer = false;
    private int lockedSlot = -1;
    private ImageButton powerButton;

    @SuppressWarnings("unused")
    public Server(Inventory playerInventory, ServerInventory serverInventory, Rack rack, int slot) {
        super(new li.cil.oc.core.impl.common.container.Server(Menus.SERVER, 0, playerInventory, serverInventory, null, playerInventory.player));
        this.serverInventory = serverInventory;
        this.rack = rack;
        this.slot = slot;
        this.resolvedRackFromContainer = true;
    }

    public Server(li.cil.oc.core.impl.common.container.Server container, Inventory inv, Component title) {
        super(container, inv, title);
        this.serverInventory = container.otherInventory instanceof ServerInventory si ? si : null;
        this.rack = resolveRackFromContainer(container);
        this.slot = Math.max(container.rackSlot, 0);
        this.resolvedRackFromContainer = container.rackPos != null;
    }

    private Rack resolveRackFromContainer(li.cil.oc.core.impl.common.container.Server container) {
        if (container.rackPos != null && Minecraft.getInstance().level != null) {
            var te = Minecraft.getInstance().level.getBlockEntity(container.rackPos);
            if (te instanceof Rack r) return r;
        }
        return null;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (!resolvedRackFromContainer) {
            var newRack = resolveRackFromContainer(getMenu());
            if (newRack != null) {
                this.rack = newRack;
                this.slot = Math.max(getMenu().rackSlot, 0);
                this.resolvedRackFromContainer = true;
            }
        }
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
    public ItemStack lockedStack() {
        return serverInventory != null ? serverInventory.container() : ItemStack.EMPTY;
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 81 && hoveredSlot != null && shouldSuppressClick(hoveredSlot)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        if (rack != null) {
            var current = rack.getItem(slot);
            var original = serverInventory != null ? serverInventory.container() : ItemStack.EMPTY;
            if (current.isEmpty() || original.isEmpty() || !current.is(original.getItem())) {
                Minecraft.getInstance().setScreen(null);
                return;
            }
        }

        if (powerButton != null) {
            powerButton.visible = !getMenu().isItem;
            powerButton.toggled = getMenu().isRunning;
        }
        super.render(guiGraphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        powerButton = new ImageButton(0, leftPos + 48, topPos + 33, 18, 18, Textures.guiButtonPower, true);
        addRenderableWidget(powerButton);
        powerButton.setPressHandler(this::actionPerformed);
    }

    protected void actionPerformed(ImageButton button) {
        if (button.getId() == 0) {
            if (rack != null) {
                PacketSender.sendServerPower(rack, slot, !getMenu().isRunning);
            }
        }
    }

    @SuppressWarnings("unused")
    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);
        if (serverInventory != null) {
            guiGraphics.drawString(font, Component.translatable(serverInventory.getInventoryName()).getString(), 8, 6, 0x404040, false);
        } else {
            guiGraphics.drawString(font, Component.translatable("container.opencomputers.server").getString(), 8, 6, 0x404040, false);
        }
        if (powerButton != null && powerButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            var key = getMenu().isRunning
                    ? Component.translatable("gui.opencomputers.robot.turnoff").getString()
                    : Component.translatable("gui.opencomputers.robot.turnon").getString();
            for (String line : key.split("\n")) {
                tooltip.add(Component.literal(line));
            }
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics guiGraphics) {
        guiGraphics.blit(Textures.guiServer, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
