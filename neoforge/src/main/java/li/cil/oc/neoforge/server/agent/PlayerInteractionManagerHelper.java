package li.cil.oc.neoforge.server.agent;

import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.UpgradeExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

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
        if (player instanceof ServerPlayer sp && player instanceof li.cil.oc.neoforge.server.agent.Player fp) {
            final boolean[] hasExperienceUpgrade = {false};
            var machineNode = fp.agent.machine().node();
            if (machineNode != null) {
                for (Node n : machineNode.reachableNodes()) {
                    if (n.canBeReachedFrom(machineNode) && n.host() instanceof UpgradeExperience) {
                        hasExperienceUpgrade[0] = true;
                        break;
                    }
                }
            }

            final int[] expToDrop = {0};
            final boolean[] broken = {false};

            var handler = new Object() {
                @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
                @SuppressWarnings("unused")
                public void onBlockDrops(BlockDropsEvent e) {
                    if (e.getBreaker() == player) {
                        if (hasExperienceUpgrade[0]) {
                            expToDrop[0] += e.getDroppedExperience();
                            e.setDroppedExperience(0);
                        }
                    }
                }
            };

            NeoForge.EVENT_BUS.register(handler);
            try {
                broken[0] = sp.gameMode.destroyBlock(pos);
            } catch (Exception e) {
                return -1;
            } finally {
                NeoForge.EVENT_BUS.unregister(handler);
            }

            if (broken[0]) {
                return expToDrop[0];
            }
        }
        return -1;
    }
}
