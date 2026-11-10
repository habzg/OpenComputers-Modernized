package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.multipart.block.TileMultipart;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.neoforge.common.MultipartHooks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;

public final class MultipartHooksImpl implements MultipartHooks.Access {
  public static final MultipartHooksImpl INSTANCE = new MultipartHooksImpl();

  private MultipartHooksImpl() {
  }

  @Override
  public int getCableColor(BlockEntity te) {
    if (!(te instanceof TileMultipart tileMP)) return Color.LightGray;
    for (var part : tileMP.getPartList()) {
      if (part instanceof CablePart cablePart) return cablePart.getColor();
    }
    return Color.LightGray;
  }

  @Override
  public boolean hasOCPart(BlockEntity te) {
    if (!(te instanceof TileMultipart tileMP)) return false;
    for (var part : tileMP.getPartList()) {
      if (part instanceof li.cil.oc.api.network.Environment) return true;
    }
    return false;
  }

  @Override
  public boolean canConnect(BlockEntity te, Direction side) {
    if (!(te instanceof TileMultipart tileMP)) return true;
    return MultipartNetworkBridge.canConnectFromSide(tileMP, side);
  }

  @Override
  public boolean isCableHit(BlockEntity te, HitResult hit) {
    if (!(te instanceof TileMultipart)) return false;
    if (!(hit instanceof PartRayTraceResult partHit)) return false;
    return partHit.part instanceof CablePart;
  }
}
