package li.cil.oc.core.impl.client.gui;

import li.cil.oc.api.internal.TextBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BufferScreen extends Screen {
  private final TextBuffer buffer;

  protected BufferScreen(TextBuffer buffer, Component title) {
    super(title);
    this.buffer = buffer;
  }

  public TextBuffer buffer() {
    return buffer;
  }

  public static void closeIfOpen(TextBuffer buffer) {
    var mc = Minecraft.getInstance();
    if (mc.screen instanceof BufferScreen screen && screen.buffer() == buffer) {
      mc.setScreen(null);
    }
  }
}
