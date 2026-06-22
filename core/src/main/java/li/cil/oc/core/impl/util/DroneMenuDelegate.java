package li.cil.oc.core.impl.util;

import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface DroneMenuDelegate {
    void openMenu(Player player, Object drone);
}
