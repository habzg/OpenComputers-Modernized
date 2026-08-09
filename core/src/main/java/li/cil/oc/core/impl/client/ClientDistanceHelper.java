package li.cil.oc.core.impl.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClientDistanceHelper {
    private static DistanceHelper distanceHelper;
    private static ProjectHelper projectHelper;

    public static void setDistanceHelper(DistanceHelper helper) {
        distanceHelper = helper;
    }

    public static void setProjectHelper(ProjectHelper helper) {
        projectHelper = helper;
    }

    public static double distanceSquared(Level level, double x, double y, double z, Entity entity) {
        final var helper = distanceHelper;
        if (helper != null) {
            return helper.distanceSquared(level, x, y, z, entity.getX(), entity.getY(), entity.getZ());
        }
        return entity.distanceToSqr(x, y, z);
    }

    public static Vec3 project(Level level, Vec3 pos) {
        final var helper = projectHelper;
        return helper != null ? helper.project(level, pos) : pos;
    }

    @FunctionalInterface
    public interface DistanceHelper {
        double distanceSquared(Level level, double x, double y, double z, double px, double py, double pz);
    }

    @FunctionalInterface
    public interface ProjectHelper {
        Vec3 project(Level level, Vec3 pos);
    }

    private ClientDistanceHelper() {
    }
}