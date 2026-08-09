package li.cil.oc.fabric.util;

import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.util.DroneMenuDelegate;
import li.cil.oc.fabric.common.init.Menus;
import li.cil.oc.fabric.common.network.MenuData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public class DroneMenuOpener implements DroneMenuDelegate {
    public static final DroneMenuOpener INSTANCE = new DroneMenuOpener();

    private DroneMenuOpener() {
    }

    @Override
    public void openMenu(Player player, Object droneObj) {
        if (!(droneObj instanceof Drone drone)) return;
        player.openMenu(new ExtendedScreenHandlerFactory<MenuData>() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.literal("Drone");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
                return new li.cil.oc.core.impl.common.container.Drone(Menus.DRONE, id, inv, drone);
            }

            @Override
            public MenuData getScreenOpeningData(@NotNull ServerPlayer player) {
                return new MenuData(li.cil.oc.core.common.GuiType.Drone, drone.getId(), 0, 0, "");
            }
        });
    }
}
