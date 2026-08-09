package li.cil.oc.fabric.server.component;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.server.component.DiskDriveMountableBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.fabric.common.network.MenuData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public class DiskDriveMountable extends DiskDriveMountableBase {
    public DiskDriveMountable(Rack rack, int slot) {
        super(rack, slot);
    }

    @Override
    protected void openDiskDriveGui(@NotNull Player player, @NotNull BlockPosition pos, int slot) {
        player.openMenu(new ExtendedScreenHandlerFactory<MenuData>() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("container.opencomputers.diskdrive");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                return new li.cil.oc.core.impl.common.container.DiskDrive(
                        li.cil.oc.fabric.common.init.Menus.DISK_DRIVE, id, inv,
                        (net.minecraft.world.Container) rack.getMountable(slot));
            }

            @Override
            public MenuData getScreenOpeningData(@NotNull ServerPlayer p) {
                return new MenuData(GuiType.DiskDriveMountableInRack, pos.x(), GuiType.embedSlot(pos.y(), slot), pos.z(), "");
            }
        });
    }
}
