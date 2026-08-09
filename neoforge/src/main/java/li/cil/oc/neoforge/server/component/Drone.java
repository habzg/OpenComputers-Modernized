package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.core.impl.server.component.DroneBase;
import li.cil.oc.core.impl.util.ExtendedArguments;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class Drone extends DroneBase {
    public final li.cil.oc.core.impl.common.entity.Drone agent;

    public Drone(li.cil.oc.core.impl.common.entity.Drone agent) {
        super(agent.inventorySize());
        this.agent = agent;
    }

    @Override
    public li.cil.oc.api.internal.@NotNull Agent agent() {
        return agent;
    }

    @Override
    public @NotNull Direction checkSideForAction(@NotNull Arguments args, int n) {
        return ExtendedArguments.checkSideAny(args, n);
    }

    @Override
    protected @NotNull Player createRotatedPlayer(@NotNull Direction facing, @NotNull Direction side) {
        li.cil.oc.neoforge.server.agent.Player player = (li.cil.oc.neoforge.server.agent.Player) agent.player();
        li.cil.oc.neoforge.server.agent.Player.updatePositionAndRotation(player, facing, side);
        return player;
    }

    @Override
    protected boolean postRobotPlaceInAirEvent() {
        RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(agent);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event.isAllowed();
    }

    @Override
    protected int playerActivateBlockOrUseItem(@NotNull Player player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration) {
        return ((li.cil.oc.neoforge.server.agent.Player) player).activateBlockOrUseItem(x, y, z, side, hitX, hitY, hitZ, duration);
    }

    @Override
    protected boolean playerUseEquippedItem(@NotNull Player player, double duration) {
        return ((li.cil.oc.neoforge.server.agent.Player) player).useEquippedItem(duration);
    }

    @Override
    protected boolean playerPlaceBlock(@NotNull Player player, int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return player instanceof li.cil.oc.neoforge.server.agent.Player p && p.placeBlock(slot, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    protected void setPlayerSneaking(@NotNull Player player, boolean sneaky) {
        player.setShiftKeyDown(sneaky);
    }

    @Override
    protected double playerBreakBlock(@NotNull Player player, int x, int y, int z, int dir) {
        return ((li.cil.oc.neoforge.server.agent.Player) player).clickBlock(x, y, z, dir);
    }

    @Override
    protected void playerAttackEntity(@NotNull Player player, @NotNull Entity target) {
        ((li.cil.oc.neoforge.server.agent.Player) player).attackTargetEntityWithCurrentItem(target);
    }

    @Override
    protected @NotNull Entity playerClosestEntity(@NotNull Player player, @NotNull Direction facing, @NotNull Class<? extends Entity> cls) {
        return ((li.cil.oc.neoforge.server.agent.Player) player).closestEntity(facing, cls);
    }

    @Override
    protected @NotNull HitResult playerPick(@NotNull Player player, double range) {
        li.cil.oc.neoforge.server.agent.Player p = (li.cil.oc.neoforge.server.agent.Player) player;
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

    @Override
    protected @NotNull String statusTextImpl() {
        return agent.statusText();
    }

    @Override
    protected void setStatusTextImpl(@NotNull String value) {
        agent.statusText(value);
    }

    @Override
    protected int lightColorImpl() {
        return agent.lightColor();
    }

    @Override
    protected void setLightColorImpl(int value) {
        agent.lightColor(value);
    }

    @Override
    protected float targetXImpl() {
        return agent.targetX();
    }

    @Override
    protected void setTargetXImpl(float value) {
        agent.targetX(value);
    }

    @Override
    protected float targetYImpl() {
        return agent.targetY();
    }

    @Override
    protected void setTargetYImpl(float value) {
        agent.targetY(value);
    }

    @Override
    protected float targetZImpl() {
        return agent.targetZ();
    }

    @Override
    protected void setTargetZImpl(float value) {
        agent.targetZ(value);
    }

    @Override
    protected double distanceToTargetSqr() {
        return agent.distanceToSqr(agent.targetX(), agent.targetY(), agent.targetZ());
    }

    @Override
    protected double motionX() {
        return agent.getDeltaMovement().x();
    }

    @Override
    protected double motionY() {
        return agent.getDeltaMovement().y();
    }

    @Override
    protected double motionZ() {
        return agent.getDeltaMovement().z();
    }

    @Override
    protected float maxVelocity() {
        return agent.maxVelocity;
    }

    @Override
    protected float targetAccelerationImpl() {
        return agent.targetAcceleration();
    }

    @Override
    protected void setTargetAccelerationImpl(float value) {
        agent.targetAcceleration(value);
    }

    @Override
    protected void playPickupSound() {
        agent.level().playSound(null, agent.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                0.2f, (agent.level().random.nextFloat() - agent.level().random.nextFloat()) * 0.7f + 1.0f);
    }
}
