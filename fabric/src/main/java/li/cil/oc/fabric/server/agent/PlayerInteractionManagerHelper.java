package li.cil.oc.fabric.server.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.UpgradeExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class PlayerInteractionManagerHelper {
    public static boolean onBlockClicked(Player player, BlockPos pos, Direction side) {
        if (player instanceof ServerPlayer sp) {
            sp.setPos(
                    pos.getX() + 0.5 - side.getStepX() * 0.1,
                    pos.getY() + 0.5 - side.getStepY() * 0.1,
                    pos.getZ() + 0.5 - side.getStepZ() * 0.1
            );
            int maxBuildHeight = sp.level().getMaxBuildHeight();
            sp.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side, maxBuildHeight, 0);
            sp.setPos(player.getX(), player.getY(), player.getZ());
            return true;
        }
        return false;
    }

    public static int blockRemoving(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer sp && player instanceof li.cil.oc.fabric.server.agent.Player fp) {
            boolean hasExperienceUpgrade = false;
            var machineNode = fp.agent.machine().node();
            if (machineNode != null) {
                for (Node n : machineNode.reachableNodes()) {
                    if (n.canBeReachedFrom(machineNode) && n.host() instanceof UpgradeExperience) {
                        hasExperienceUpgrade = true;
                        break;
                    }
                }
            }

            ServerLevel level = sp.serverLevel();
            AABB orbArea = new AABB(pos).inflate(2);
            List<ExperienceOrb> orbsBefore = hasExperienceUpgrade
                    ? new ArrayList<>(level.getEntitiesOfClass(ExperienceOrb.class, orbArea))
                    : Collections.emptyList();

            boolean broken;
            try {
                broken = sp.gameMode.destroyBlock(pos);
            } catch (Exception e) {
                return -1;
            }

            if (broken) {
                if (hasExperienceUpgrade) {
                    int exp = 0;
                    for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, orbArea)) {
                        if (!orbsBefore.contains(orb)) {
                            exp += orb.getValue();
                            orb.discard();
                        }
                    }
                    return exp;
                }
                return 0;
            }
        }
        return -1;
    }
}
