package li.cil.oc.neoforge.client;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public final class GuiHandler {
    private GuiHandler() {
    }

    public static void handleRegisterMenuScreens(RegisterMenuScreensEvent event) {
        reg(event, Menus.ADAPTER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Adapter((li.cil.oc.core.impl.common.container.Adapter) c, i, t));
        reg(event, Menus.ASSEMBLER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Assembler((li.cil.oc.core.impl.common.container.Assembler) c, i, t));
        reg(event, Menus.CASE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Case((li.cil.oc.core.impl.common.container.Case) c, i, t));
        reg(event, Menus.CHARGER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Charger((li.cil.oc.core.impl.common.container.Charger) c, i, t));
        reg(event, Menus.DATABASE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Database((li.cil.oc.neoforge.common.container.Database) c, i, t));
        reg(event, Menus.DISASSEMBLER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Disassembler((li.cil.oc.core.impl.common.container.Disassembler) c, i, t));
        reg(event, Menus.DISK_DRIVE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.DiskDrive((li.cil.oc.core.impl.common.container.DiskDrive) c, i, t));
        reg(event, Menus.DRONE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Drone((li.cil.oc.core.impl.common.container.Drone) c, i, t));
        reg(event, Menus.PRINTER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Printer((li.cil.oc.core.impl.common.container.Printer) c, i, t));
        reg(event, Menus.RACK, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Rack((li.cil.oc.core.impl.common.container.Rack) c, i, t));
        reg(event, Menus.RAID, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Raid((li.cil.oc.core.impl.common.container.Raid) c, i, t));
        reg(event, Menus.RELAY, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Relay((li.cil.oc.neoforge.common.container.Relay) c, i, t));
        reg(event, Menus.ROBOT, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Robot((li.cil.oc.neoforge.common.container.Robot) c, i, t));
        reg(event, Menus.SERVER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Server((li.cil.oc.core.impl.common.container.Server) c, i, t));
        reg(event, Menus.TABLET, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.neoforge.client.gui.Tablet((li.cil.oc.core.impl.common.container.Tablet) c, i, t));
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void reg(RegisterMenuScreensEvent event, DeferredHolder<MenuType<?>, ? extends MenuType> holder, net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor factory) {
        event.register(holder.get(), factory);
    }

    private static net.minecraft.client.gui.screens.Screen wrapScreenBuffer(
            li.cil.oc.api.internal.TextBuffer buffer, boolean hasMouse,
            java.util.function.Supplier<Boolean> hasKeyboardCallback,
            java.util.function.Supplier<Boolean> hasPower) {
        var inner = new li.cil.oc.core.impl.client.gui.Screen(buffer, hasMouse, hasKeyboardCallback, hasPower);
        return new net.minecraft.client.gui.screens.Screen(Component.literal("screen")) {
            @Override
            public boolean isPauseScreen() {
                return false;
            }

            @Override
            protected void init() {
                super.init();
                BufferRenderer.init(Minecraft.getInstance().getTextureManager());
                inner.setGuiSize(this.width, this.height);
            }

            @Override
            public void render(net.minecraft.client.gui.@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
                renderBackground(guiGraphics, mouseX, mouseY, dt);
                inner.render(guiGraphics, mouseX, mouseY, dt);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return inner.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return inner.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return inner.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
                return inner.mouseScrolled(mouseX, mouseY, scrollDeltaY);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return inner.handleKeyPress(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
                return inner.handleKeyRelease(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                return inner.handleCharTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
            }

            @Override
            public void onClose() {
                inner.onGuiClosed();
                super.onClose();
            }
        };
    }

    public static void openScreen(int guiType, int x, int y, int z) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var world = player.level();
        switch (GuiType.Categories.get(guiType)) {
            case Block -> {
                var pos = new net.minecraft.core.BlockPos(x, GuiType.extractY(y), z);
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Screen screen && guiType == GuiType.Screen) {
                    Minecraft.getInstance().setScreen(wrapScreenBuffer(
                            screen.origin.buffer, screen.tier > 0,
                            () -> screen.origin.hasKeyboard(),
                            () -> screen.origin.buffer.isRenderingEnabled()));
                }
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Waypoint waypoint && guiType == GuiType.Waypoint) {
                    Minecraft.getInstance().setScreen(new li.cil.oc.neoforge.client.gui.Waypoint(waypoint));
                }
            }
            case Item -> {
                if (guiType == GuiType.Drive) {
                    Minecraft.getInstance().setScreen(new li.cil.oc.neoforge.client.gui.Drive(player.getInventory(), player::getMainHandItem));
                }
            }
            default -> {
            }
        }
    }
}
