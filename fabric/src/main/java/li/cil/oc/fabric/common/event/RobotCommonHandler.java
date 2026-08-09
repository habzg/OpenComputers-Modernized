package li.cil.oc.fabric.common.event;

import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.UpgradeHover;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class RobotCommonHandler {
    public static void onRobotApplyDamageRate(RobotUsedToolEvent.ApplyDamageRate e) {
        if (e.toolAfterUse.isDamageableItem()) {
            int damage = e.toolAfterUse.getDamageValue() - e.toolBeforeUse.getDamageValue();
            if (damage > 0) {
                double actualDamage = damage * e.getDamageRate();
                int repairedDamage = e.agent.player().getRandom().nextDouble() > 0.5
                        ? damage - (int) Math.floor(actualDamage)
                        : damage - (int) Math.ceil(actualDamage);
                e.toolAfterUse.setDamageValue(e.toolAfterUse.getDamageValue() - repairedDamage);
            }
        }
    }

    public static void onRobotMove(RobotMoveEvent.Pre e) {
        if (OCSettings.get().limitFlightHeight >= 0 && e.agent instanceof Robot robot) {
            var world = robot.level();
            int maxFlyingHeight = OCSettings.get().limitFlightHeight;

            for (int i = 0; i < robot.equipmentInventory().getContainerSize(); i++) {
                var stack = robot.equipmentInventory().getItem(i);
                var item = stack.getItem();
                if (item instanceof UpgradeHover hover) {
                    maxFlyingHeight = Math.max(maxFlyingHeight, OCSettings.get().upgradeFlightHeight[hover.tier()]);
                }
            }

            for (int i = 0; i < robot.componentCount(); i++) {
                var stack = robot.getItem(i + robot.mainInventory().getContainerSize() + robot.equipmentInventory().getContainerSize());
                var item = stack.getItem();
                if (item instanceof UpgradeHover hover) {
                    maxFlyingHeight = Math.max(maxFlyingHeight, OCSettings.get().upgradeFlightHeight[hover.tier()]);
                }
            }

            boolean isMovingDown = e.direction == Direction.DOWN;
            boolean bypassesFlightLimit = maxFlyingHeight >= world.getHeight();
            BlockPosition startPos = BlockPosition.apply(robot);
            BlockPosition targetPos = startPos.offset(e.direction);
            boolean validMove = isMovingDown ||
                    bypassesFlightLimit ||
                    hasAdjacentBlock(world, startPos) ||
                    hasAdjacentBlock(world, targetPos) ||
                    isWithinFlyingHeight(world, maxFlyingHeight, startPos);

            if (!validMove) {
                e.setCanceled(true);
            }
        }
    }

    private static boolean hasAdjacentBlock(Level world, BlockPosition pos) {
        for (Direction side : Direction.values()) {
            var neighborPos = pos.offset(side).toBlockPos();
            if (world.getBlockState(neighborPos).isFaceSturdy(world, neighborPos, side.getOpposite())) return true;
        }
        return false;
    }

    private static boolean isWithinFlyingHeight(Level world, int maxFlyingHeight, BlockPosition pos) {
        for (int n = 1; n <= maxFlyingHeight; n++) {
            if (!world.isEmptyBlock(pos.offset(Direction.DOWN, n).toBlockPos())) return true;
        }
        return false;
    }
}
