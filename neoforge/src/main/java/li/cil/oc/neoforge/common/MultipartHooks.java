package li.cil.oc.neoforge.common;

import li.cil.oc.core.impl.util.Color;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;

public final class MultipartHooks {
  public interface Access {
    int getCableColor(BlockEntity te);

    boolean hasOCPart(BlockEntity te);

    boolean canConnect(BlockEntity te, Direction side);

    boolean isCableHit(BlockEntity te, HitResult hit);
  }

  public static Access access;

  private MultipartHooks() {
  }

  public static int getCableColor(BlockEntity te) {
    return access != null ? access.getCableColor(te) : Color.LightGray;
  }

  public static boolean hasOCPart(BlockEntity te) {
    return access != null && access.hasOCPart(te);
  }

  public static boolean denyConnect(BlockEntity te, Direction side) {
    return access != null && !access.canConnect(te, side);
  }

  public static boolean isCableHit(BlockEntity te, HitResult hit) {
    return access != null && access.isCableHit(te, hit);
  }
}
