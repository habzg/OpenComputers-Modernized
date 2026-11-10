package li.cil.oc.fabric.integration.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import li.cil.oc.core.impl.client.ClientDistanceHelper;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.core.impl.util.SubLevelHelper;
import li.cil.oc.fabric.integration.ModProxy;
import li.cil.oc.fabric.integration.Mods;
import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public final class ModSable implements ModProxy {
  @Override
  public li.cil.oc.core.integration.Mod getMod() {
    return Mods.Sable;
  }

  @Override
  public void initialize() {
    PacketBuilderBase.setDistanceHelper(ModSable::distanceSquared);
    ClientDistanceHelper.setDistanceHelper(ModSable::distanceSquaredTo);
    ClientDistanceHelper.setProjectHelper(ModSable::project);
    SubLevelHelper.setProjector(ModSable::project);
    SubLevelHelper.setDistanceSquared(ModSable::distanceSquaredBetween);
    SubLevelHelper.setStorageCenters(ModSable::storageCenters);
  }

  private static double distanceSquared(Level level, double x, double y, double z, Player player) {
    return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, x, y, z, player.getX(), player.getY(), player.getZ());
  }

  private static double distanceSquaredTo(Level level, double x, double y, double z, double px, double py, double pz) {
    return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, x, y, z, px, py, pz);
  }

  private static double distanceSquaredBetween(Level level, Vec3 a, Vec3 b) {
    return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, a.x, a.y, a.z, b.x, b.y, b.z);
  }

  private static Vec3 project(Level level, Vec3 pos) {
    return SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) pos);
  }

  private static java.util.List<Vec3> storageCenters(SubLevelHelper.StorageQuery query) {
    var center = dev.ryanhcode.sable.companion.math.JOMLConversion.toJOML(query.center());
    var box = new dev.ryanhcode.sable.companion.math.BoundingBox3d(
      query.center().x - query.range(), query.center().y - query.range(), query.center().z - query.range(),
      query.center().x + query.range(), query.center().y + query.range(), query.center().z + query.range());
    var result = new java.util.ArrayList<Vec3>();
    for (var sub : SableCompanion.INSTANCE.getAllIntersecting(query.level(), box)) {
      var dest = new org.joml.Vector3d();
      sub.logicalPose().transformPositionInverse(center, dest);
      result.add(new Vec3(dest.x, dest.y, dest.z));
    }
    return result;
  }
}
