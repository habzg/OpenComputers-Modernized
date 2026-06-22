package li.cil.oc.neoforge.common.init;

import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.GuiHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Menus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, OpenComputers.ID);
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> ADAPTER = register("adapter");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> ASSEMBLER = register("assembler");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> CASE = register("case");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> CHARGER = register("charger");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> DATABASE = register("database");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> DISASSEMBLER = register("disassembler");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> DISK_DRIVE = register("disk_drive");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> DRONE = register("drone");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> PRINTER = register("printer");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> RACK = register("rack");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> RAID = register("raid");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> RELAY = register("relay");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> ROBOT = register("robot");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> SERVER = register("server");
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> TABLET = register("tablet");

    private Menus() {
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> register(String name) {
        return MENU_TYPES.register(name, () ->
                IMenuTypeExtension.create((id, inv, buf) -> {
                    int guiType = buf.readInt();
                    int x = buf.readInt();
                    int y = buf.readInt();
                    int z = buf.readInt();
                    return (T) GuiHandler.getServerGuiElement(id, guiType, inv.player, inv.player.level(), x, y, z);
                }));
    }
}
