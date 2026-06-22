package li.cil.oc.core.impl.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
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
}
