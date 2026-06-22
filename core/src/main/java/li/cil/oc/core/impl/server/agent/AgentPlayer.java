package li.cil.oc.core.impl.server.agent;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public interface AgentPlayer {
    @SuppressWarnings("unused")
    Direction facing();

    @SuppressWarnings("unused")
    Direction side();

    double clickBlock(int x, int y, int z, int side);

    void attackTargetEntityWithCurrentItem(Entity target);

    boolean placeBlock(int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ);

    <T extends Entity> T closestEntity(Direction facing, Class<T> cls);

    HitResult pick(double range);

    int activateBlockOrUseItem(int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration);

    boolean useEquippedItem(double duration);
}
