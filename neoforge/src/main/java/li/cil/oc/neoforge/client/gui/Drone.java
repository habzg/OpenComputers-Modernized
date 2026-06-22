package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.client.gui.widget.ProgressBar;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.client.renderer.font.TextBufferRenderData;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.impl.util.TextBuffer;
import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.client.renderer.tileentity.ScreenBufferTextRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Drone extends DynamicGuiContainer<li.cil.oc.neoforge.common.container.Drone> implements li.cil.oc.core.impl.client.gui.traits.DisplayBuffer {
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
        super(new li.cil.oc.neoforge.common.container.Drone(0, playerInventory, drone));
        this.drone = drone;
        imageWidth = 176;
        imageHeight = 148;
        power = addWidget(new ProgressBar(28, 48));
    }

    public Drone(li.cil.oc.neoforge.common.container.Drone container, Inventory inv, net.minecraft.network.chat.Component title) {
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
    public double changeSize(double w, double h, boolean recompile) {
        return 2.0;
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawBufferLayer(guiGraphics);
        renderBufferText(guiGraphics);
        if (isHovering(power.x, power.y, power.width(), power.height(), mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            String format = Component.translatable("gui.opencomputers.robot.power").getString() + ": %d%% (%d/%d)";
            tooltip.add(String.format(format,
                    drone.globalBuffer() * 100 / Math.max(drone.globalBufferSize(), 1),
                    drone.globalBuffer(),
                    drone.globalBufferSize()));
            renderTooltip(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        if (powerButton.isHoveredOrFocused()) {
            List<String> tooltip = new ArrayList<>(Arrays.asList(
                    (drone.isRunning() ? Component.translatable("gui.opencomputers.robot.turnoff").getString() : Component.translatable("gui.opencomputers.robot.turnon").getString()).split("\n")));
            renderTooltip(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float dt, int mouseX, int mouseY) {
        guiGraphics.blit(Textures.guiDrone, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        power.level = (double) drone.globalBuffer() / Math.max(drone.globalBufferSize(), 1.0);
        drawWidgets(guiGraphics);
        if (drone.mainInventory.getContainerSize() > 0) drawSelection(guiGraphics);
    }

    private void drawSelection(GuiGraphics guiGraphics) {
        int slot = drone.selectedSlot();
        int cols = Math.min(4, drone.mainInventory.getContainerSize());
        if (slot >= 0 && slot < drone.mainInventory.getContainerSize()) {
            double now = System.currentTimeMillis() / 1000.0;
            double selectionStepV = 1 / 17.0;
            int offsetV = (int) ((now - (int) now) * 17) * (int) (selectionStepV * 256);
            int x = leftPos + 98 - 1 + (slot % cols) * 18;
            int y = topPos + 8 - 1 + (slot / cols) * 18;
            guiGraphics.blit(Textures.guiRobotSelection, x, y, (int) blitOffset, 0.0f, (float) offsetV, 20, 17, 256, 256);
        }
    }
}
