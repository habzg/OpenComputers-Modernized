package li.cil.oc.core.impl.client.gui;

import li.cil.oc.core.impl.client.gui.widget.WidgetContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class CustomGuiContainer<C extends AbstractContainerMenu> extends AbstractContainerScreen<C> implements WidgetContainer {
  protected float blitOffset;

  @SuppressWarnings({"unused", "DataFlowIssue"})
  public CustomGuiContainer(C inventoryContainer) {
    super(inventoryContainer, null, null);
  }

  @SuppressWarnings("unused")
  public CustomGuiContainer(C inventoryContainer, Inventory inv, Component title) {
    super(inventoryContainer, inv, title);
  }

  public int windowX() {
    return leftPos;
  }

  public int windowY() {
    return topPos;
  }

  public int getXSize() {
    return imageWidth;
  }

  @SuppressWarnings("unused")
  public float windowZ() {
    return blitOffset;
  }
}
