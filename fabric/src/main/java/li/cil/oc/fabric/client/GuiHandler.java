package li.cil.oc.fabric.client;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public final class GuiHandler {
    private GuiHandler() {
    }

    public static void registerScreens() {
        reg(li.cil.oc.fabric.common.init.Menus.ADAPTER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Adapter(
                (li.cil.oc.core.impl.common.container.Adapter) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.ASSEMBLER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Assembler(
                (li.cil.oc.core.impl.common.container.Assembler) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.CASE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Case(
                (li.cil.oc.core.impl.common.container.Case) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.CHARGER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Charger(
                (li.cil.oc.core.impl.common.container.Charger) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.DATABASE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Database(
                (li.cil.oc.fabric.common.container.Database) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.DISASSEMBLER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Disassembler(
                (li.cil.oc.core.impl.common.container.Disassembler) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.DISK_DRIVE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.DiskDrive(
                (li.cil.oc.core.impl.common.container.DiskDrive) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.DRONE, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Drone(
                (li.cil.oc.core.impl.common.container.Drone) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.PRINTER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Printer(
                (li.cil.oc.core.impl.common.container.Printer) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.RACK, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Rack(
                (li.cil.oc.core.impl.common.container.Rack) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.RAID, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Raid(
                (li.cil.oc.core.impl.common.container.Raid) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.RELAY, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Relay(
                (li.cil.oc.fabric.common.container.Relay) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.ROBOT, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Robot(
                (li.cil.oc.fabric.common.container.Robot) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.SERVER, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Server(
                (li.cil.oc.core.impl.common.container.Server) c, i, t));
        reg(li.cil.oc.fabric.common.init.Menus.TABLET, (AbstractContainerMenu c, Inventory i, Component t) -> new li.cil.oc.fabric.client.gui.Tablet(
                (li.cil.oc.core.impl.common.container.Tablet) c, i, t));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void reg(net.minecraft.world.inventory.MenuType type, MenuScreens.ScreenConstructor factory) {
        MenuScreens.register(type, factory);
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
            public void render(@NotNull net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
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
                    Minecraft.getInstance().setScreen(new li.cil.oc.fabric.client.gui.Waypoint(waypoint));
                }
            }
            case Item -> {
                if (guiType == GuiType.Drive) {
                    Minecraft.getInstance().setScreen(new li.cil.oc.fabric.client.gui.Drive(
                            player.getInventory(), player::getMainHandItem));
                }
            }
            default -> {
            }
        }
    }
}
