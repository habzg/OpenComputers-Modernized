package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.impl.common.blockentity.DiskDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

public final class SneakBypass {
  private SneakBypass() {
  }

  public static boolean bypasses(ItemStack stack, BlockGetter level, BlockPos pos) {
    if (stack.isEmpty()) return true;
    var item = stack.getItem();
    return switch (item) {
      case FloppyDisk ignored -> true;
      case Wrench ignored -> true;
      case EEPROM ignored -> true;
      case HoverBoots ignored -> level.getBlockEntity(pos) instanceof DiskDrive;
      default -> false;
    };
  }
}
