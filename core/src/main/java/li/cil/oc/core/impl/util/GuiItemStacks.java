package li.cil.oc.core.impl.util;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.common.item.DiskDriveMountable;
import li.cil.oc.core.impl.common.item.Server;
import li.cil.oc.core.impl.common.item.Tablet;
import li.cil.oc.core.impl.common.item.UpgradeDatabase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GuiItemStacks {
  private GuiItemStacks() {
  }

  public static ItemStack inUse(int guiType, Player player) {
    ItemStack mainHand = player.getMainHandItem();
    if (matches(guiType, mainHand)) return mainHand;
    return player.getOffhandItem();
  }

  private static boolean matches(int guiType, ItemStack stack) {
    var item = stack.getItem();
    if (guiType == GuiType.Database) return item instanceof UpgradeDatabase;
    if (guiType == GuiType.Server) return item instanceof Server;
    if (guiType == GuiType.Tablet || guiType == GuiType.TabletInner) return item instanceof Tablet;
    if (guiType == GuiType.DiskDriveMountable) return item instanceof DiskDriveMountable;
    return false;
  }
}
