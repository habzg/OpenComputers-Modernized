package li.cil.oc.neoforge.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class Robot extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Robot> implements li.cil.oc.core.impl.client.gui.traits.InputBuffer {
    public final li.cil.oc.neoforge.common.blockentity.Robot robot;
    public final String address;
    public final int deltaY;
    private li.cil.oc.api.internal.TextBuffer buffer;
    private String bufferAddress;
    private final ProgressBar power;
    private ImageButton powerButton;
    private ImageButton scrollButton;
    private int inventoryOffset = 0;
    private boolean isDragging = false;
    private double currentScale = 0;

    @SuppressWarnings("unused")
    public Robot(Inventory playerInventory, li.cil.oc.neoforge.common.blockentity.Robot robot) {
        this(new li.cil.oc.neoforge.common.container.Robot(0, playerInventory, robot), playerInventory, net.minecraft.network.chat.Component.literal("Robot"));
    }

    public Robot(li.cil.oc.neoforge.common.container.Robot container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.robot = container.robot;
        this.address = container.address;
        deltaY = container.deltaY;
        imageWidth = 256;
        imageHeight = 256 - deltaY;
        power = addWidget(new ProgressBar(26, 156 - deltaY));

        buffer = findBuffer();
    }

    private li.cil.oc.neoforge.common.blockentity.Robot current() {
        var level = robot.getLevel();
        if (level != null && address != null && !address.isEmpty()) {
            var resolved = li.cil.oc.core.impl.common.container.RobotLookup.get(level, address);
            if (resolved instanceof li.cil.oc.neoforge.common.blockentity.Robot lr) return lr;
        }
        var clientLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (clientLevel != null && clientLevel != level && address != null && !address.isEmpty()) {
            var resolved = li.cil.oc.core.impl.common.container.RobotLookup.get(clientLevel, address);
            if (resolved instanceof li.cil.oc.neoforge.common.blockentity.Robot lr) return lr;
        }
        return robot;
    }

    private li.cil.oc.api.internal.TextBuffer findBuffer() {
        var r = current();
        var buf = r.agentComponents().stream()
                .filter(c -> c instanceof li.cil.oc.api.internal.TextBuffer)
                .map(c -> (li.cil.oc.api.internal.TextBuffer) c)
                .findFirst().orElse(null);
        if (buf != null) {
            bufferAddress = ((li.cil.oc.core.impl.common.component.TextBufferBase) buf).proxy.nodeAddress;
        }
        return buf;
    }

    @Override
    public double scale() {
        if (currentScale <= 0) {
            changeSize(bufferColumns(), bufferRows(), true);
        }
        return currentScale;
    }

    @Override
    public li.cil.oc.api.internal.TextBuffer buffer() {
        var r = current();
        var buf = r.agentComponents().stream()
                .filter(c -> c instanceof li.cil.oc.api.internal.TextBuffer)
                .map(c -> (li.cil.oc.api.internal.TextBuffer) c)
                .findFirst().orElse(null);
        if (buf != null) {
            buffer = buf;
            bufferAddress = ((li.cil.oc.core.impl.common.component.TextBufferBase) buf).proxy.nodeAddress;
            return buf;
        }
        if (bufferAddress != null && !bufferAddress.isEmpty()) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                var managed = li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.get(level, bufferAddress);
                if (managed instanceof li.cil.oc.api.internal.TextBuffer tb) {
                    buffer = tb;
                    return tb;
                }
            }
        }
        return buffer;
    }

    @Override
    @SuppressWarnings("unused")
    public boolean hasKeyboard() {
        var r = current();
        return r != null && r.agentComponents().stream()
                .anyMatch(c -> c instanceof li.cil.oc.api.internal.Keyboard);
    }

    @Override
    public int bufferX() {
        return (int) (8 + (240 - bufferRenderWidth()) / 2);
    }

    @Override
    public int bufferY() {
        return (int) (8 + (140 - bufferRenderHeight()) / 2);
    }

    @Override
    public int bufferColumns() {
        if (buffer != null) return buffer.getViewportWidth();
        return 80;
    }

    @Override
    public int bufferRows() {
        if (buffer != null) return buffer.getViewportHeight();
        return 16;
    }

    private double bufferRenderWidth() {
        return Math.min(240.0, TextBufferRenderCache.renderer.charRenderWidth() * OCSettings.screenResolutionsByTier[0][0]);
    }

    private double bufferRenderHeight() {
        return Math.min(140.0, TextBufferRenderCache.renderer.charRenderHeight() * OCSettings.screenResolutionsByTier[0][1]);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return handleKeyPress(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return handleKeyRelease(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return handleCharTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled = isRunning();
        scrollButton.active = canScroll();
        scrollButton.hoverOverride = isDragging;
        if (current().inventorySize < 16 + inventoryOffset * 4) scrollTo(0);
        super.render(guiGraphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        if (buffer == null) buffer = findBuffer();
        initGui();
        powerButton = new ImageButton(0, leftPos + 5, topPos + 153 - deltaY, 18, 18, Textures.guiButtonPower, true);
        scrollButton = new ImageButton(1, leftPos + 169 + 18 * 4 + 2 + 1, topPos + 155 - deltaY + 1, 6, 13, Textures.guiButtonScroll);
        addRenderableWidget(powerButton);
        addRenderableWidget(scrollButton);
        powerButton.setPressHandler(this::onPowerButton);
        scrollTo(0);
    }

    private boolean isRunning() {
        return current().isRunning;
    }

    @SuppressWarnings("unused")
    private void onPowerButton(ImageButton button) {
        var r = current();
        li.cil.oc.neoforge.client.PacketSender.sendComputerPower(r, !r.isRunning);
    }

    public void drawGui(GuiGraphics guiGraphics) {
        buffer();
        if (buffer != null) {
            int vw = buffer.getViewportWidth();
            int vh = buffer.getViewportHeight();
            if (vw <= 0 || vh <= 0) return;

            double rw = buffer.renderWidth();
            double rh = buffer.renderHeight();
            if (rw <= 0 || rh <= 0) return;

            double scaleX = bufferRenderWidth() / rw;
            double scaleY = bufferRenderHeight() / rh;
            double fitScale = Math.min(scaleX, scaleY);

            var pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(bufferX(), bufferY(), 0);

            Matrix4f transform = new Matrix4f(pose.last().pose());

            if (scaleX > fitScale) {
                transform.translate((float) (buffer.renderWidth() * (scaleX - fitScale) / 2), 0, 0);
            } else if (scaleY > fitScale) {
                transform.translate(0, (float) (buffer.renderHeight() * (scaleY - fitScale) / 2), 0);
            }

            transform.scale((float) fitScale, (float) fitScale, (float) fitScale);
            transform.scale((float) this.scale(), (float) this.scale(), 1.0f);

            BufferRenderer.drawGui(guiGraphics, buffer, vw, vh, transform, current().isRunning, 1.0f, leftPos + bufferX() - 3, topPos + bufferY() - 3);

            pose.popPose();
        }
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawBufferLayer(guiGraphics);
        drawGui(guiGraphics);
        var r = current();
        if (isHovering(power.x, power.y, power.width(), power.height(), mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            String format = Component.translatable("gui.opencomputers.robot.power").getString() + ": %d%% (%d/%d)";
            tooltip.add(Component.literal(String.format(format,
                    (int) ((r.globalBuffer / r.globalBufferSize) * 100),
                    (int) r.globalBuffer, (int) r.globalBufferSize)));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
        if (powerButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            for (String line : (isRunning() ? Component.translatable("gui.opencomputers.robot.turnoff").getString() : Component.translatable("gui.opencomputers.robot.turnon").getString()).split("\n")) {
                tooltip.add(Component.literal(line));
            }
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float ignoredDt, int ignoredMouseX, int ignoredMouseY) {
        buffer();
        guiGraphics.blit(buffer != null ? Textures.guiRobot : Textures.guiRobotNoScreen, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        power.level = current().globalBuffer / current().globalBufferSize;
        drawWidgets(guiGraphics);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (current().inventorySize > 0) {
            guiGraphics.flush();
            drawSelection(guiGraphics);
        }
        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (canScroll() && button == 0 && isCoordinateOverScrollBar((int) mouseX - leftPos, (int) mouseY - topPos)) {
            isDragging = true;
            scrollMouse((int) mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            scrollMouse((int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void scrollMouse(int mouseY) {
        int scrollY = 155 - deltaY;
        scrollTo((int) Math.round((mouseY - topPos - scrollY + 1 - 6.5) * maxOffset() / (94 - 13.0)));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double ignoredScrollX, double scrollY) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        int scrollYPos = 155 - deltaY;
        if ((mx >= 169 && mx < 169 + 18 * 4 && my >= scrollYPos && my < scrollYPos + 18 * 4) ||
                (mx > 169 + 18 * 4 + 2 && mx < 169 + 18 * 4 + 2 + 8 && my >= scrollYPos && my < scrollYPos + 94)) {
            if (scrollY < 0) scrollDown();
            else scrollUp();
        }
        return true;
    }

    private boolean isCoordinateOverScrollBar(int x, int y) {
        int scrollY = 155 - deltaY;
        return x >= 169 + 18 * 4 + 2 && x <= 169 + 18 * 4 + 2 + 6 && y >= scrollY + 1 && y <= scrollY + 94 - 2;
    }

    private boolean canScroll() {
        return current().mainInventory().getContainerSize() > 16;
    }

    private int maxOffset() {
        return Math.max(0, current().mainInventory().getContainerSize() / 4 - 4);
    }

    private void scrollUp() {
        scrollTo(inventoryOffset - 1);
    }

    private void scrollDown() {
        scrollTo(inventoryOffset + 1);
    }

    private void scrollTo(int row) {
        inventoryOffset = Math.clamp(row, 0, Math.max(0, maxOffset()));
        for (int index = 4; index < 68; index++) {
            var slot = menu.getSlot(index);
            int displayIndex = index - inventoryOffset * 4 - 4;
            if (displayIndex >= 0 && displayIndex < 16) {
                slot.x = 1 + 169 + (displayIndex % 4) * 18;
                slot.y = 1 + 155 - deltaY + (displayIndex / 4) * 18;
            } else {
                slot.x = -10000;
                slot.y = -10000;
            }
        }
        int scrollY = 155 - deltaY;
        int scrollHeight = 94;
        int yMin = topPos + scrollY + 1;
        if (maxOffset() > 0) scrollButton.setY(yMin + (scrollHeight - 15) * inventoryOffset / maxOffset());
        else scrollButton.setY(yMin);
    }

    @Override
    public double changeSize(double w, double h, boolean recompile) {
        double bw = w * TextBufferRenderCache.renderer.charRenderWidth();
        double bh = h * TextBufferRenderCache.renderer.charRenderHeight();
        double scaleX = Math.min(bufferRenderWidth() / bw, 1);
        double scaleY = Math.min(bufferRenderHeight() / bh, 1);
        if (recompile && buffer != null) {
            BufferRenderer.compileBackground((int) bufferRenderWidth(), (int) bufferRenderHeight(), true);
        }
        currentScale = Math.min(scaleX, scaleY);
        return currentScale;
    }

    private void drawSelection(GuiGraphics guiGraphics) {
        int slot = current().selectedSlot - inventoryOffset * 4;
        if (slot >= 0 && slot < 16) {
            double now = System.currentTimeMillis() / 1000.0;
            float selectionStepV = 1.0f / 17.0f;
            float v0 = (int) ((now - (int) now) * 17) * selectionStepV;
            int x = 169 - 1 + (slot % 4) * 18;
            int y = 155 - deltaY - 1 + (slot / 4) * 18;
            float v1 = v0 + 20f / 340f;
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Textures.guiRobotSelection);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix = guiGraphics.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.addVertex(matrix, x, y, 0).setUv(0f, v0);
            buffer.addVertex(matrix, x, y + 20, 0).setUv(0f, v1);
            buffer.addVertex(matrix, x + 20, y + 20, 0).setUv(1f, v1);
            buffer.addVertex(matrix, x + 20, y, 0).setUv(1f, v0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    public void onClose() {
        onGuiClosed();
        super.onClose();
    }
}
