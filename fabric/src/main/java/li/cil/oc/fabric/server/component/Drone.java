package li.cil.oc.fabric.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.core.impl.server.component.DroneBase;
import li.cil.oc.core.impl.util.ExtendedArguments;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
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
        li.cil.oc.fabric.server.agent.Player player = (li.cil.oc.fabric.server.agent.Player) agent.player();
        li.cil.oc.fabric.server.agent.Player.updatePositionAndRotation(player, facing, side);
        return player;
    }

    @Override
    protected boolean postRobotPlaceInAirEvent() {
        var event = new li.cil.oc.api.event.RobotPlaceInAirEvent(agent);
        li.cil.oc.api.event.RobotPlaceInAirEvent.EVENT.invoker().onRobotPlaceInAir(event);
        return event.isAllowed();
    }

    @Override
    protected int playerActivateBlockOrUseItem(@NotNull Player player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration) {
        return ((li.cil.oc.fabric.server.agent.Player) player).activateBlockOrUseItem(x, y, z, side, hitX, hitY, hitZ, duration);
    }

    @Override
    protected boolean playerUseEquippedItem(@NotNull Player player, double duration) {
        return ((li.cil.oc.fabric.server.agent.Player) player).useEquippedItem(duration);
    }

    @Override
    protected boolean playerPlaceBlock(@NotNull Player player, int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return ((li.cil.oc.fabric.server.agent.Player) player).placeBlock(slot, x, y, z, side, hitX, hitY, hitZ);
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
    protected void playerAttackEntity(@NotNull Player player, @NotNull Entity target) {
        ((li.cil.oc.fabric.server.agent.Player) player).attackTargetEntityWithCurrentItem(target);
    }

    @Override
    protected @NotNull Entity playerClosestEntity(@NotNull Player player, @NotNull Direction facing, @NotNull Class<? extends Entity> cls) {
        return ((li.cil.oc.fabric.server.agent.Player) player).closestEntity(facing, cls);
    }

    @Override
    protected @NotNull HitResult playerPick(@NotNull Player player, double range) {
        return ((li.cil.oc.fabric.server.agent.Player) player).pick(range);
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
