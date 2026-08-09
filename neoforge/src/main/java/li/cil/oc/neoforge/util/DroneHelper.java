package li.cil.oc.neoforge.util;

import li.cil.oc.neoforge.server.agent.Player;
import net.minecraft.core.Direction;

public class DroneHelper extends li.cil.oc.core.impl.util.DroneHelper {
    @Override
    public net.minecraft.world.entity.player.Player createPlayer(Object drone) {
        if (drone instanceof li.cil.oc.api.internal.Agent agent && agent.level() instanceof net.minecraft.server.level.ServerLevel level) {
            return new Player(level, agent);
        }
        return null;
    }

    @Override
    public void updatePlayerPosition(net.minecraft.world.entity.player.Player player, Direction facing, Direction side) {
        if (player instanceof Player p) {
            Player.updatePositionAndRotation(p, facing, side);
        }
    }

    @Override
    public void setPlayerInventoryItems(net.minecraft.world.entity.player.Player player) {
        if (player instanceof Player p) {
            Player.setInventoryPlayerItems(p);
        }
    }
}
