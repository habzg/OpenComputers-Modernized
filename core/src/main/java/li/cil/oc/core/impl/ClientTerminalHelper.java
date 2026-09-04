package li.cil.oc.core.impl;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.core.impl.client.gui.Screen;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import li.cil.oc.core.impl.common.component.TerminalServer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ClientTerminalHelper {
  public static void openTerminalScreen(Player player, ItemStack stack, String key, String address) {
    var terminal = TerminalServer.TerminalServerCache.loaded.find(address);
    if (terminal == null) {
      player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.outofrange"));
      return;
    }
    var rack = terminal.rack;
    if (rack == null) {
      player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.outofrange"));
      return;
    }
    var range = terminal.range;
    var dx = player.getX() - rack.xPosition();
    var dy = player.getY() - rack.yPosition();
    var dz = player.getZ() - rack.zPosition();
    if (!player.isAlive() || dx * dx + dy * dy + dz * dz > range * range) {
      player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.outofrange"));
      return;
    }
    if (!terminal.sidedKeys().contains(key)) {
      player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.invalidkey"));
      return;
    }
    var initialBuffer = terminal.buffer();
    var initialNode = initialBuffer != null ? initialBuffer.node() : null;
    var bufferAddress = initialNode != null ? initialNode.address() : null;
    if (bufferAddress == null || bufferAddress.isEmpty()) {
      bufferAddress = terminal.address();
    }

    final String lockedBufferAddress = bufferAddress;
    final TerminalServer lockedTerminal = terminal;
    final ItemStack lockedStack = stack;
    final String lockedKey = key;

    var inner = new Screen(initialBuffer, true, () -> true, () -> {
      var cd = lockedStack.get(DataComponents.CUSTOM_DATA);
      var currentKey = (cd != null && !cd.isEmpty()) ? cd.copyTag().getString(OCSettings.namespace + "key") : "";
      if (!lockedKey.equals(currentKey)) {
        Minecraft.getInstance().setScreen(null);
        return false;
      }
      var r = lockedTerminal.rack;
      if (!player.isAlive()) {
        Minecraft.getInstance().setScreen(null);
        return false;
      }
      var dx2 = player.getX() - r.xPosition();
      var dy2 = player.getY() - r.yPosition();
      var dz2 = player.getZ() - r.zPosition();
      var range2 = lockedTerminal.range;
      if (dx2 * dx2 + dy2 * dy2 + dz2 * dz2 > range2 * range2) {
        Minecraft.getInstance().setScreen(null);
        return false;
      }
      return true;
    }) {
      @Override
      public TextBuffer buffer() {
        var mc = Minecraft.getInstance();
        var lvl = mc.level;
        if (lvl != null && lockedBufferAddress != null && !lockedBufferAddress.isEmpty()) {
          var found = li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.get(lvl, lockedBufferAddress);
          if (found instanceof TextBuffer tb) return tb;
        }
        return super.buffer();
      }
    };
    Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.Screen(Component.literal("terminal")) {
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
        return inner.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
      }

      @Override
      public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inner.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
      }

      @Override
      public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return inner.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }

      @Override
      public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        return inner.mouseScrolled(mouseX, mouseY, scrollDeltaY) || super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
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
    });
  }
}
