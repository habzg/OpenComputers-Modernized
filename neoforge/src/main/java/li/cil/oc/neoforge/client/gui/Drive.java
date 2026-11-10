package li.cil.oc.neoforge.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.common.item.data.DriveData;
import li.cil.oc.neoforge.client.PacketSender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class Drive extends Screen {
  private final Inventory playerInventory;
  private final java.util.function.Supplier<net.minecraft.world.item.ItemStack> driveStack;
  private ImageButton managedButton;
  private ImageButton unmanagedButton;
  private ImageButton lockedButton;
  private int guiLeft;
  private int guiTop;
  private final int xSize = 176;
  private final int ySize = 120;

  public Drive(Inventory playerInventory, java.util.function.Supplier<net.minecraft.world.item.ItemStack> driveStack) {
    super(Component.literal(""));
    this.playerInventory = playerInventory;
    this.driveStack = driveStack;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
    renderTransparentBackground(guiGraphics);
    var pose = guiGraphics.pose();
    pose.pushPose();
    pose.translate(guiLeft, guiTop, 0.0F);
    pose.scale(1.0F, 120.0F / 111.0F, 1.0F);
    guiGraphics.blit(Textures.guiDrive, 0, 0, 0.0F, 0.0F, 176, 111, 176, 111);
    pose.popPose();
  }

  @Override
  protected void init() {
    super.init();
    guiLeft = (width - xSize) / 2;
    guiTop = (height - ySize) / 2;
    managedButton = new ImageButton(0, guiLeft + 11, guiTop + 11, 74, 18, Textures.guiButtonDriveMode,
      Component.translatable("gui.opencomputers.drive.managed").getString(), 0x608060, true);
    unmanagedButton = new ImageButton(1, guiLeft + 91, guiTop + 11, 74, 18, Textures.guiButtonDriveMode,
      Component.translatable("gui.opencomputers.drive.unmanaged").getString(), 0x608060, true);
    lockedButton = new ImageButton(2, guiLeft + 11, guiTop + ySize - 42, 44, 18, Textures.guiButtonDriveMode,
      Component.translatable("gui.opencomputers.drive.readonlylock").getString(), 0x608060, true);
    addRenderableWidget(managedButton);
    addRenderableWidget(unmanagedButton);
    addRenderableWidget(lockedButton);
    for (var btn : new ImageButton[]{managedButton, unmanagedButton, lockedButton}) {
      btn.setPressHandler(this::actionPerformed);
    }
    updateButtonStates();
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
    super.render(guiGraphics, mouseX, mouseY, dt);
    guiGraphics.drawWordWrap(font, Component.translatable("gui.opencomputers.drive.warning"), guiLeft + 11, guiTop + 37, xSize - 20, 0x404040);
    guiGraphics.drawWordWrap(font, Component.translatable("gui.opencomputers.drive.readonlylockwarning"), guiLeft + 61, guiTop + ySize - 48, xSize - 68, 0x404040);
  }

  private void actionPerformed(ImageButton button) {
    if (button == managedButton) {
      PacketSender.sendDriveMode(false);
      DriveData.setUnmanaged(driveStack.get(), false);
    } else if (button == unmanagedButton) {
      PacketSender.sendDriveMode(true);
      DriveData.setUnmanaged(driveStack.get(), true);
    } else if (button == lockedButton) {
      PacketSender.sendDriveLock();
      DriveData.lock(driveStack.get(), playerInventory.player);
    }
    updateButtonStates();
  }

  private void updateButtonStates() {
    var data = new DriveData(driveStack.get());
    unmanagedButton.toggled = data.isUnmanaged;
    managedButton.toggled = !unmanagedButton.toggled;
    lockedButton.toggled = data.isLocked();
    lockedButton.active = !data.isLocked();
  }
}
