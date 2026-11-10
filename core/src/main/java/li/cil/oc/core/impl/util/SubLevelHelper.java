package li.cil.oc.core.impl.util;

import java.util.function.BiFunction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SubLevelHelper {
  private SubLevelHelper() {
  }

  private static BiFunction<Level, Vec3, Vec3> projector = (level, pos) -> pos;

  public static void setProjector(BiFunction<Level, Vec3, Vec3> projector) {
    SubLevelHelper.projector = projector;
  }

  public static Vec3 projectOut(Level level, Vec3 pos) {
    Vec3 projected = projector.apply(level, pos);
    return projected != null ? projected : pos;
  }

  public interface DistanceFn {
    double apply(Level level, Vec3 a, Vec3 b);
  }

  private static DistanceFn distanceSquared = (level, a, b) -> a.distanceToSqr(b);

  public static void setDistanceSquared(DistanceFn distanceSquared) {
    SubLevelHelper.distanceSquared = distanceSquared;
  }

  public static double distanceSquared(Level level, Vec3 a, Vec3 b) {
    return distanceSquared.apply(level, a, b);
  }

  public record StorageQuery(Level level, Vec3 center, double range) {
  }

  private static java.util.function.Function<StorageQuery, java.util.List<Vec3>> storageCenters = q -> java.util.List.of();

  public static void setStorageCenters(java.util.function.Function<StorageQuery, java.util.List<Vec3>> storageCenters) {
    SubLevelHelper.storageCenters = storageCenters;
  }

  public static java.util.List<Vec3> storageCenters(Level level, Vec3 center, double range) {
    var result = storageCenters.apply(new StorageQuery(level, center, range));
    return result != null ? result : java.util.List.of();
  }
}
