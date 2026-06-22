package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.DroneHelper;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class Robot extends li.cil.oc.core.impl.server.component.RobotBase {
    public final li.cil.oc.core.impl.common.tileentity.RobotBase agent;

    public Robot(li.cil.oc.core.impl.common.tileentity.RobotBase agent) {
        super(String.valueOf(agent.getContainerSize()));
        this.agent = agent;
    }

    @Override
    public li.cil.oc.api.internal.@NotNull Agent agent() {
        return agent;
    }

    @Override
    protected Direction toGlobal(Direction value) {
        return agent.toGlobal(value);
    }

    @Override
    protected void animateSwing(double duration) {
        agent.animateSwing(duration);
    }

    @Override
    protected int getLightColor() {
        return agent.info.lightColor;
    }

    @Override
    protected void setLightColor(int value) {
        agent.info.lightColor = value;
        PacketSender.sendRobotLightChange(agent, value);
    }

    @Override
    protected ItemStack getEquipmentInSlot(int slot) {
        return agent.equipmentInventory.getItem(slot);
    }

    @Override
    protected boolean isAnimatingMove() {
        return agent.isAnimatingMove();
    }

    @Override
    protected boolean tryMove(Direction direction) {
        return agent.move(direction);
    }

    @Override
    protected void rotateProxy(Direction axis) {
        agent.rotateProxy(axis);
    }

    @Override
    protected void animateTurn(boolean clockwise, double duration) {
        agent.animateTurn(clockwise, duration);
    }

    @Override
    protected void sendParticleEffect(BlockPosition pos, String name, int count, double speed, Direction dir) {
        PacketSender.sendParticleEffect(pos, name, count, speed, dir);
    }

    @Override
    protected void sendToReachable(String message, Object data) {
        agent.sendToReachable(message, data);
    }

    @Override
    protected Node getAgentNode() {
        return agent.node();
    }

    @Override
    protected Player createRotatedPlayer(Direction facing, Direction side) {
        var player = agent.player();
        if (DroneHelper.get() != null)
            DroneHelper.get().updatePlayerPosition(player, facing, side);
        return player;
    }

    @Override
    protected boolean postRobotPlaceInAirEvent() {
        li.cil.oc.api.event.RobotPlaceInAirEvent event = li.cil.oc.api.event.OCEventFactory.get().createRobotPlaceInAirEvent(agent);
        if (EventHandlerDelegate.get() != null) EventHandlerDelegate.get().post(event);
        return event.isAllowed();
    }

    @Override
    protected void setPlayerSneaking(Player player, boolean sneaky) {
        player.setShiftKeyDown(sneaky);
    }

    @Override
    protected double playerBreakBlock(Player player, int x, int y, int z, int dir) {
        return player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap ? ap.clickBlock(x, y, z, dir) : 0;
    }

    @Override
    protected boolean playerPlaceBlock(Player player, int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap && ap.placeBlock(slot, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    protected void playerAttackEntity(Player player, Entity target) {
        if (player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap)
            ap.attackTargetEntityWithCurrentItem(target);
    }

    @Override
    protected Entity playerClosestEntity(Player player, Direction facing, Class<? extends Entity> cls) {
        return player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap ? ap.closestEntity(facing, cls) : null;
    }

    @Override
    protected HitResult playerPick(Player player, double range) {
        return player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap ? ap.pick(range) : null;
    }

    @Override
    protected int playerActivateBlockOrUseItem(Player player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration) {
        return player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap ? ap.activateBlockOrUseItem(x, y, z, side, hitX, hitY, hitZ, duration) : li.cil.oc.core.server.agent.ActivationType.None;
    }

    @Override
    protected boolean playerUseEquippedItem(Player player, double duration) {
        return player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer ap && ap.useEquippedItem(duration);
    }

    @Override
    protected void beginConsumeDrops(Entity entity) {
    }

    @Override
    protected void endConsumeDrops(Player player, Entity entity) {
    }
}
