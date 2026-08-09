package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.ClientDistanceHelper;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.neoforge.client.PacketSender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class Waypoint extends Screen {
    public final li.cil.oc.core.impl.common.blockentity.Waypoint waypoint;
    private EditBox textField;

    public Waypoint(li.cil.oc.core.impl.common.blockentity.Waypoint waypoint) {
        super(Component.literal("waypoint"));
        this.waypoint = waypoint;
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) return;
        if (ClientDistanceHelper.distanceSquared(waypoint.getLevel(), waypoint.getBlockPos().getX() + 0.5, waypoint.getBlockPos().getY() + 0.5, waypoint.getBlockPos().getZ() + 0.5, minecraft.player) > 64) {
            minecraft.player.closeContainer();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        int midX = width / 2;
        int midY = height / 2;
        int guiWidth = 176;
        int guiHeight = 24;
        int guiLeft = midX - guiWidth / 2;
        int guiTop = midY - guiHeight / 2;
        textField = new EditBox(font, guiLeft + 7, guiTop + 8, 164 - 12, 12, Component.empty());
        textField.setMaxLength(32);
        textField.setBordered(false);
        textField.setCanLoseFocus(false);
        textField.setFocused(true);
        textField.setTextColor(0xFFFFFF);
        textField.setValue(waypoint.label);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft == null || minecraft.player == null) return false;
        if (textField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            String label = textField.getValue().substring(0, Math.min(32, textField.getValue().length()));
            if (!label.equals(waypoint.label)) {
                waypoint.label = label;
                PacketSender.sendWaypointLabel(waypoint);
                minecraft.player.closeContainer();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textField.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        super.render(guiGraphics, mouseX, mouseY, dt);
        int midX = width / 2;
        int midY = height / 2;
        int guiWidth = 176;
        int guiHeight = 24;
        int guiLeft = midX - guiWidth / 2;
        int guiTop = midY - guiHeight / 2;
        GL11.glColor3f(1, 1, 1);
        guiGraphics.blit(Textures.guiWaypoint, guiLeft, guiTop, 0, 0, guiWidth, guiHeight);
        textField.render(guiGraphics, mouseX, mouseY, dt);
    }
}
