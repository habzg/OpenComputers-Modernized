package li.cil.oc.core.impl.util;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record BlockPosition(int x, int y, int z, Level level) {

    public BlockPosition(double x, double y, double z, Level level) {
        this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z), level);
    }

    public BlockPosition(double x, double y, double z) {
        this(x, y, z, null);
    }

    public static BlockPosition apply(int x, int y, int z, Level level) {
        return new BlockPosition(x, y, z, level);
    }

    public static BlockPosition apply(double x, double y, double z, Level level) {
        return new BlockPosition(x, y, z, level);
    }

    public static BlockPosition apply(EnvironmentHost host) {
        return apply(host.xPosition(), host.yPosition(), host.zPosition(), host.level());
    }

    public static BlockPosition apply(Entity entity) {
        return apply(entity.getX(), entity.getY(), entity.getZ(), entity.level());
    }

    public BlockPosition offset(Direction direction, int n) {
        return new BlockPosition(
                x + direction.getStepX() * n,
                y + direction.getStepY() * n,
                z + direction.getStepZ() * n,
                level
        );
    }

    public BlockPosition offset(Direction direction) {
        return offset(direction, 1);
    }

    public Vec3 offset(double x, double y, double z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }

    public AABB bounds() {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public Vec3 toVec3() {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }
}
