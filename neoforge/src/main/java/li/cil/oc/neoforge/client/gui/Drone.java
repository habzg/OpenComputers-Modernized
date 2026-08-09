package li.cil.oc.neoforge.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.font.TextBufferRenderData;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.impl.util.TextBuffer;
import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.client.renderer.blockentity.ScreenBufferTextRenderer;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class Drone extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Drone> implements li.cil.oc.core.impl.client.gui.traits.DisplayBuffer {
    public final li.cil.oc.core.impl.common.entity.Drone drone;
    private final TextBuffer buffer = new TextBuffer(20, 2, new PackedColor.SingleBitFormat(0x33FF33));
    private final TextBufferRenderData bufferRenderer = new TextBufferRenderData() {
        private boolean _dirty = true;

        @Override
        public boolean dirty() {
            return _dirty;
        }

        @Override
        public void setDirty(boolean value) {
            _dirty = value;
        }

        @Override
        public TextBuffer data() {
            return buffer;
        }

        @Override
        public int[] viewport() {
            return new int[]{buffer.size()[0], buffer.size()[1]};
        }
    };
    private final ProgressBar power;
    private ImageButton powerButton;

    @SuppressWarnings("unused")
    public Drone(Inventory playerInventory, li.cil.oc.core.impl.common.entity.Drone drone) {
        super(new li.cil.oc.core.impl.common.container.Drone(Menus.DRONE.get(), 0, playerInventory, drone));
        this.drone = drone;
        imageWidth = 176;
        imageHeight = 148;
        power = addWidget(new ProgressBar(28, 48));
    }

    public Drone(li.cil.oc.core.impl.common.container.Drone container, Inventory inv, net.minecraft.network.chat.Component title) {
        super(container, inv, title);
        this.drone = container.drone;
        imageWidth = 176;
        imageHeight = 148;
        power = addWidget(new ProgressBar(28, 48));
    }

    @Override
    public int bufferX() {
        return 9;
    }

    @Override
    public int bufferY() {
        return 9;
    }

    @Override
    public int bufferColumns() {
        return 80;
    }

    @Override
    public int bufferRows() {
        return 16;
    }

    private void onPowerButton() {
        PacketSender.sendDronePower(drone, !drone.isRunning());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled = drone.isRunning();
        var lines = drone.statusText().split("\n");
        for (int i = 0; i < lines.length; i++) {
            buffer.set(0, i, lines[i], false);
        }
        bufferRenderer.setDirty(lines.length > 0);
        super.render(guiGraphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        powerButton = new ImageButton(0, leftPos + 7, topPos + 45, 18, 18, Textures.guiButtonPower, true);
        addRenderableWidget(powerButton);
        powerButton.setPressHandler(button -> onPowerButton());
    }

    private void renderBufferText(GuiGraphics guiGraphics) {
        TextBufferRenderCache.generateChars(bufferRenderer);
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(bufferX(), bufferY(), 0);
        pose.scale((float) scale(), (float) scale(), 1);
        Matrix4f transform = new Matrix4f(pose.last().pose());
        var vp = bufferRenderer.viewport();
        ScreenBufferTextRenderer.render(guiGraphics.bufferSource(), buffer, vp[0], vp[1], transform, 0.5f, 0.5f, 1.0f, 1.0f);
        pose.popPose();
    }

    @Override
    public double changeSize(double ignoredW, double ignoredH, boolean ignoredRecompile) {
        return 2.0;
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawBufferLayer(guiGraphics);
        renderBufferText(guiGraphics);
        if (isHovering(power.x, power.y, power.width(), power.height(), mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            String format = Component.translatable("gui.opencomputers.robot.power").getString() + ": %d%% (%d/%d)";
            tooltip.add(Component.literal(String.format(format,
                    drone.globalBuffer() * 100 / Math.max(drone.globalBufferSize(), 1),
                    drone.globalBuffer(),
                    drone.globalBufferSize())));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
        if (powerButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            for (String line : (drone.isRunning() ? Component.translatable("gui.opencomputers.robot.turnoff").getString() : Component.translatable("gui.opencomputers.robot.turnon").getString()).split("\n")) {
                tooltip.add(Component.literal(line));
            }
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float ignoredDt, int ignoredMouseX, int ignoredMouseY) {
        guiGraphics.blit(Textures.guiDrone, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        power.level = (double) drone.globalBuffer() / Math.max(drone.globalBufferSize(), 1.0);
        drawWidgets(guiGraphics);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (drone.mainInventory.getContainerSize() > 0) {
            guiGraphics.flush();
            drawSelection(guiGraphics);
        }
        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    private void drawSelection(GuiGraphics guiGraphics) {
        int slot = drone.selectedSlot();
        int cols = Math.min(4, drone.mainInventory.getContainerSize());
        if (slot >= 0 && slot < drone.mainInventory.getContainerSize()) {
            double now = System.currentTimeMillis() / 1000.0;
            float selectionStepV = 1.0f / 17.0f;
            float v0 = (int) ((now - (int) now) * 17) * selectionStepV;
            int x = 96 + (slot % cols) * 18;
            int y = 6 + (slot / cols) * 18;
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
}
