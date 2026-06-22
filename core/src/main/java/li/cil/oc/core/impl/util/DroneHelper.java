package li.cil.oc.core.impl.util;

import net.minecraft.world.entity.player.Player;

public abstract class DroneHelper {
    private static DroneHelper instance;

    public static void setInstance(DroneHelper inst) {
        instance = inst;
    }

    public static DroneHelper get() {
        return instance;
    }

    public abstract Player createPlayer(Object drone);

    public abstract void updatePlayerPosition(Player player, net.minecraft.core.Direction facing, net.minecraft.core.Direction side);

    public abstract void setPlayerInventoryItems(Player player);
}
