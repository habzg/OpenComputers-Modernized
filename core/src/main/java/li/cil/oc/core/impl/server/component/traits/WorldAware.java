package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;


public interface WorldAware {
    BlockPosition position();

    default Level level() {
        return position().level();
    }

    default Player fakePlayer() {
        return WorldAction.getFakePlayer(level(), position());
    }

    default boolean mayInteract(BlockPosition blockPos, Direction face) {
        try {
            return WorldAction.mayInteract(level(), blockPos, face);
        } catch (Throwable t) {
            Log.get().warn("Some event handler threw up while checking for permission to access a block.", t);
            return true;
        }
    }

    default <T extends Entity> List<T> entitiesInBounds(AABB bounds, Class<T> type) {
        return level().getEntitiesOfClass(type, bounds);
    }

    default <T extends Entity> List<T> entitiesInBlock(BlockPosition blockPos, Class<T> type) {
        return entitiesInBounds(blockPos.bounds(), type);
    }

    default <T extends Entity> List<T> entitiesOnSide(Direction side, Class<T> type) {
        return entitiesInBlock(position().offset(side), type);
    }

    default <T extends Entity> T closestEntity(Direction side, Class<T> type) {
        BlockPosition blockPos = position().offset(side);
        var entities = level().getEntitiesOfClass(type, blockPos.bounds());
        if (entities.isEmpty()) return null;
        return entities.getFirst();
    }

    default Object[] blockContent(Direction side) {
        Entity closest = closestEntity(side, Entity.class);
        if (closest instanceof LivingEntity || closest instanceof Minecart) {
            return new Object[]{true, "entity"};
        }
        BlockPosition blockPos = position().offset(side);
        BlockPos pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) {
            return new Object[]{false, "air"};
        }
        if (!state.getFluidState().isEmpty()) {
            return new Object[]{WorldAction.checkBlockBreak(level(), pos, state), "liquid"};
        }
        if (state.canBeReplaced()) {
            return new Object[]{WorldAction.checkBlockBreak(level(), pos, state), "replaceable"};
        }
        if (state.getCollisionShape(level(), pos).isEmpty()) {
            return new Object[]{true, "passable"};
        }
        return new Object[]{true, "solid"};
    }
}
