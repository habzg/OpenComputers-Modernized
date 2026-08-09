package li.cil.oc.fabric.server.component;

import li.cil.oc.core.impl.server.component.AgentBase;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public abstract class Agent extends AgentBase {
    @Override
    protected li.cil.oc.fabric.server.agent.@NotNull Player createRotatedPlayer(@NotNull Direction facing, @NotNull Direction side) {
        li.cil.oc.fabric.server.agent.Player player = (li.cil.oc.fabric.server.agent.Player) agent().player();
        li.cil.oc.fabric.server.agent.Player.updatePositionAndRotation(player, facing, side);
        return player;
    }

    @Override
    protected boolean postRobotPlaceInAirEvent() {
        var event = new li.cil.oc.api.event.RobotPlaceInAirEvent(agent());
        li.cil.oc.api.event.RobotPlaceInAirEvent.EVENT.invoker().onRobotPlaceInAir(event);
        return event.isAllowed();
    }

    @Override
    protected void setPlayerSneaking(@NotNull Player player, boolean sneaky) {
        player.setShiftKeyDown(sneaky);
    }

    @Override
    protected double playerBreakBlock(@NotNull Player player, int x, int y, int z, int dir) {
        return ((li.cil.oc.fabric.server.agent.Player) player).clickBlock(x, y, z, dir);
    }

    @Override
    protected boolean playerPlaceBlock(@NotNull Player player, int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return ((li.cil.oc.fabric.server.agent.Player) player).placeBlock(slot, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    protected void playerAttackEntity(@NotNull Player player, @NotNull Entity target) {
        ((li.cil.oc.fabric.server.agent.Player) player).attackTargetEntityWithCurrentItem(target);
    }

    @Override
    protected @NotNull Entity playerClosestEntity(@NotNull Player player, @NotNull Direction facing, @NotNull Class<? extends Entity> cls) {
        return ((li.cil.oc.fabric.server.agent.Player) player).closestEntity(facing, cls);
    }

    @Override
    protected @NotNull HitResult playerPick(@NotNull Player player, double range) {
        return pick(player, range);
    }

    @Override
    protected int playerActivateBlockOrUseItem(@NotNull Player player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration) {
        return ((li.cil.oc.fabric.server.agent.Player) player).activateBlockOrUseItem(x, y, z, side, hitX, hitY, hitZ, duration);
    }

    @Override
    protected boolean playerUseEquippedItem(@NotNull Player player, double duration) {
        return ((li.cil.oc.fabric.server.agent.Player) player).useEquippedItem(duration);
    }

    protected @NotNull HitResult pick(@NotNull Player player, double range) {
        li.cil.oc.fabric.server.agent.Player p = (li.cil.oc.fabric.server.agent.Player) player;
        Vec3 origin = new Vec3(
                p.getX() + p.facing.getStepX() * 0.5,
                p.getY() + p.facing.getStepY() * 0.5,
                p.getZ() + p.facing.getStepZ() * 0.5);
        Vec3 blockCenter = origin.add(
                p.facing.getStepX() * 0.51,
                p.facing.getStepY() * 0.51,
                p.facing.getStepZ() * 0.51);
        Vec3 target = blockCenter.add(
                p.side.getStepX() * range,
                p.side.getStepY() * range,
                p.side.getStepZ() * range);
        HitResult hit = p.level().clip(new net.minecraft.world.level.ClipContext(origin, target, net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        Entity closest = p.closestEntity(p.facing, Entity.class);
        if ((closest instanceof LivingEntity || closest instanceof Minecart || closest instanceof li.cil.oc.core.impl.common.entity.Drone)) {
            if (new Vec3(p.getX(), p.getY(), p.getZ()).distanceTo(hit.getLocation()) > p.distanceTo(closest)) {
                return new EntityHitResult(closest);
            }
        }
        return hit;
    }
}
