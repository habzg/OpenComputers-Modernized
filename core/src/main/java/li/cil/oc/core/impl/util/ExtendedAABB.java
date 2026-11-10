package li.cil.oc.core.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ExtendedAABB {
  public static AABB unitBounds() {
    return new AABB(0, 0, 0, 1, 1, 1);
  }

  public static int volume(AABB bounds) {
    int sx = (int) Math.round((bounds.maxX - bounds.minX) * 16);
    int sy = (int) Math.round((bounds.maxY - bounds.minY) * 16);
    int sz = (int) Math.round((bounds.maxZ - bounds.minZ) * 16);
    return sx * sy * sz;
  }

  public static AABB rotateTowards(AABB bounds, Direction facing) {
    int count = switch (facing) {
      case WEST -> 3;
      case NORTH -> 2;
      case EAST -> 1;
      default -> 0;
    };
    return rotateY(bounds, count);
  }

  public static AABB rotateY(AABB bounds, int count) {
    Vec3 min = new Vec3(bounds.minX - 0.5, bounds.minY - 0.5, bounds.minZ - 0.5);
    Vec3 max = new Vec3(bounds.maxX - 0.5, bounds.maxY - 0.5, bounds.maxZ - 0.5);
    min = min.yRot(count * (float) Math.PI * 0.5f);
    max = max.yRot(count * (float) Math.PI * 0.5f);
    return new AABB(
      Math.round(Math.min(min.x + 0.5, max.x + 0.5) * 32) / 32f,
      Math.round(Math.min(min.y + 0.5, max.y + 0.5) * 32) / 32f,
      Math.round(Math.min(min.z + 0.5, max.z + 0.5) * 32) / 32f,
      Math.round(Math.max(min.x + 0.5, max.x + 0.5) * 32) / 32f,
      Math.round(Math.max(min.y + 0.5, max.y + 0.5) * 32) / 32f,
      Math.round(Math.max(min.z + 0.5, max.z + 0.5) * 32) / 32f
    );
  }

  /*
  * This is pulled from 1.12's Vec3D class, it's needed as modern Minecraft seems
  * to handle inside hits differently, which is incompatible with how
  * the Robot decides where it can place a block.
  * */
  public static HitResult resolveInsideHit(Vec3 origin, Vec3 target, HitResult hit) {
    if (!(hit instanceof BlockHitResult blockHit) || !blockHit.isInside()) {
      return hit;
    }
    BlockPos pos = blockHit.getBlockPos();
    double ox = origin.x - pos.getX();
    double oy = origin.y - pos.getY();
    double oz = origin.z - pos.getZ();
    double ex = target.x - pos.getX();
    double ey = target.y - pos.getY();
    double ez = target.z - pos.getZ();
    Vec3 bestPoint = null;
    Direction bestFace = null;
    double[][] faces = {
      {0, 0}, {1, 0}, {0, 1}, {1, 1}, {0, 2}, {1, 2}
    };
    Direction[] faceDirs = {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
    for (int f = 0; f < 6; f++) {
      double plane = faces[f][0];
      int axis = (int) faces[f][1];
      double s = axis == 0 ? ox : (axis == 1 ? oy : oz);
      double e = axis == 0 ? ex : (axis == 1 ? ey : ez);
      double d0 = e - s;
      if (d0 * d0 < 1.0000000116860974E-7D) {
        continue;
      }
      double d3 = (plane - s) / d0;
      if (d3 < 0.0D || d3 > 1.0D) {
        continue;
      }
      double px = ox + (ex - ox) * d3;
      double py = oy + (ey - oy) * d3;
      double pz = oz + (ez - oz) * d3;
      double a1 = axis == 0 ? py : px;
      double a2 = axis == 2 ? py : pz;
      if (a1 < 0.0D || a1 > 1.0D || a2 < 0.0D || a2 > 1.0D) {
        continue;
      }
      if (bestPoint == null || origin.distanceToSqr(new Vec3(px + pos.getX(), py + pos.getY(), pz + pos.getZ())) < origin.distanceToSqr(new Vec3(bestPoint.x + pos.getX(), bestPoint.y + pos.getY(), bestPoint.z + pos.getZ()))) {
        bestPoint = new Vec3(px, py, pz);
        bestFace = faceDirs[f];
      }
    }
    if (bestPoint == null) {
      return BlockHitResult.miss(target, blockHit.getDirection(), pos);
    }
    return new BlockHitResult(new Vec3(bestPoint.x + pos.getX(), bestPoint.y + pos.getY(), bestPoint.z + pos.getZ()), bestFace, pos, false);
  }
}
