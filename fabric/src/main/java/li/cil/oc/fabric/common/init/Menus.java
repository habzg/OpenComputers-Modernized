package li.cil.oc.fabric.common.init;

import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.common.GuiHandler;
import li.cil.oc.fabric.common.network.MenuData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class Menus {
    private Menus() {
    }

    private static ExtendedScreenHandlerType<AbstractContainerMenu, MenuData> register(String name) {
        ExtendedScreenHandlerType<AbstractContainerMenu, MenuData> type = new ExtendedScreenHandlerType<>(
            (int syncId, Inventory inv, MenuData data) ->
                GuiHandler.getServerGuiElement(syncId, data.guiType(), inv.player, inv.player.level(), data.x(), data.y(), data.z(), data.address()),
            MenuData.CODEC
        );
        Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name), type);
        return type;
    }

    public static final MenuType<AbstractContainerMenu> ADAPTER = register("adapter");
    public static final MenuType<AbstractContainerMenu> ASSEMBLER = register("assembler");
    public static final MenuType<AbstractContainerMenu> CASE = register("case");
    public static final MenuType<AbstractContainerMenu> CHARGER = register("charger");
    public static final MenuType<AbstractContainerMenu> DATABASE = register("database");
    public static final MenuType<AbstractContainerMenu> DISASSEMBLER = register("disassembler");
    public static final MenuType<AbstractContainerMenu> DISK_DRIVE = register("disk_drive");
    public static final MenuType<AbstractContainerMenu> DRONE = register("drone");
    public static final MenuType<AbstractContainerMenu> PRINTER = register("printer");
    public static final MenuType<AbstractContainerMenu> RACK = register("rack");
    public static final MenuType<AbstractContainerMenu> RAID = register("raid");
    public static final MenuType<AbstractContainerMenu> RELAY = register("relay");
    public static final MenuType<AbstractContainerMenu> ROBOT = register("robot");
    public static final MenuType<AbstractContainerMenu> SERVER = register("server");
    public static final MenuType<AbstractContainerMenu> TABLET = register("tablet");

    public static void init() {
    }
}
