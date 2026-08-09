package li.cil.oc.neoforge.integration.opencomputers;

import java.util.Arrays;
import java.util.HashSet;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.internal.Wrench;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.prefab.ResourceContentProvider;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.renderer.markdown.segment.render.BlockImageProvider;
import li.cil.oc.core.impl.client.renderer.markdown.segment.render.ItemImageProvider;
import li.cil.oc.core.impl.client.renderer.markdown.segment.render.OreDictImageProvider;
import li.cil.oc.core.impl.client.renderer.markdown.segment.render.TextureImageProvider;
import li.cil.oc.core.impl.common.block.SimpleBlock;
import li.cil.oc.core.impl.common.item.DelegateItem;
import li.cil.oc.core.impl.common.nanomachines.provider.HungryProvider;
import li.cil.oc.core.impl.common.nanomachines.provider.MagnetProvider;
import li.cil.oc.core.impl.common.nanomachines.provider.ParticleProvider;
import li.cil.oc.core.impl.common.nanomachines.provider.PotionProvider;
import li.cil.oc.core.impl.common.template.DroneTemplate;
import li.cil.oc.core.impl.common.template.MicrocontrollerTemplate;
import li.cil.oc.core.impl.common.template.NavigationUpgradeTemplate;
import li.cil.oc.core.impl.common.template.RobotTemplate;
import li.cil.oc.core.impl.common.template.ServerTemplate;
import li.cil.oc.core.impl.common.template.TabletTemplate;
import li.cil.oc.core.impl.common.template.TemplateBlacklist;
import li.cil.oc.core.impl.integration.opencomputers.ConverterLinkedCard;
import li.cil.oc.core.impl.integration.opencomputers.ConverterNanomachines;
import li.cil.oc.core.impl.integration.opencomputers.DriverComponentBus;
import li.cil.oc.core.impl.integration.opencomputers.DriverContainerCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverContainerFloppy;
import li.cil.oc.core.impl.integration.opencomputers.DriverContainerUpgrade;
import li.cil.oc.core.impl.integration.opencomputers.DriverDataCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverEEPROM;
import li.cil.oc.core.impl.integration.opencomputers.DriverGeolyzer;
import li.cil.oc.core.impl.integration.opencomputers.DriverGraphicsCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverInternetCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverKeyboard;
import li.cil.oc.core.impl.integration.opencomputers.DriverLinkedCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverMemory;
import li.cil.oc.core.impl.integration.opencomputers.DriverMotionSensor;
import li.cil.oc.core.impl.integration.opencomputers.DriverNetworkCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverScreen;
import li.cil.oc.core.impl.integration.opencomputers.DriverTerminalServer;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeAngel;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeBarcodeReader;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeBattery;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeExperience;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeGenerator;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeHover;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeInventory;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeLeash;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradePiston;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeSolarGenerator;
import li.cil.oc.core.impl.integration.opencomputers.DriverWirelessNetworkCard;
import li.cil.oc.core.impl.integration.util.WirelessRedstone;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.neoforge.common.EventHandler;
import li.cil.oc.neoforge.common.Loot;
import li.cil.oc.neoforge.common.SaveHandler;
import li.cil.oc.neoforge.common.asm.SimpleComponentTickHandler;
import li.cil.oc.neoforge.common.event.AngelUpgradeHandler;
import li.cil.oc.neoforge.common.event.BlockChangeHandler;
import li.cil.oc.neoforge.common.event.ChunkloaderUpgradeHandler;
import li.cil.oc.neoforge.common.event.ExperienceUpgradeHandler;
import li.cil.oc.neoforge.common.event.FileSystemAccessHandler;
import li.cil.oc.neoforge.common.event.HoverBootsHandler;
import li.cil.oc.neoforge.common.event.NanomachinesHandler;
import li.cil.oc.neoforge.common.event.NetworkActivityHandler;
import li.cil.oc.neoforge.common.event.RobotCommonHandler;
import li.cil.oc.neoforge.common.event.WirelessNetworkCardHandler;
import li.cil.oc.neoforge.common.nanomachines.provider.DisintegrationProvider;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import li.cil.oc.neoforge.integration.util.JEI;
import li.cil.oc.neoforge.server.component.Drone;
import li.cil.oc.neoforge.server.network.Waypoints;
import li.cil.oc.neoforge.server.network.WirelessNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;

@SuppressWarnings("unused")
public final class ModOpenComputers implements ModProxy {
    public static boolean useWrench(Player player, int x, int y, int z, boolean changeDurability) {
        if (player.getMainHandItem().getItem() instanceof Wrench wrench) {
            return wrench.useWrenchOnBlock(player, player.level(), new BlockPos(x, y, z), !changeDurability);
        }
        return false;
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof Wrench;
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack.getItem() instanceof Chargeable chargeable) {
            return chargeable.canCharge(stack);
        }
        return false;
    }

    public static double charge(ItemStack stack, double amount, boolean simulate) {
        if (stack.getItem() instanceof Chargeable chargeable) {
            return chargeable.charge(stack, amount, simulate);
        }
        return amount;
    }

    public static int inkCartridgeInkProvider(ItemStack stack) {
        if (li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.InkCartridge)) {
            return OCSettings.get().printInkValue;
        }
        return 0;
    }

    public static int dyeInkProvider(ItemStack stack) {
        if (Color.isDye(stack)) {
            return OCSettings.get().printInkValue / 10;
        }
        return 0;
    }

    private static void blacklistHost(Class<?> host, String... itemNames) {
        for (String itemName : itemNames) {
            ItemInfo itemInfo = li.cil.oc.api.Items.get(itemName);
            if (itemInfo != null) {
                li.cil.oc.core.impl.common.Registrar.blacklistHost(itemName, host, itemInfo.createItemStack(1));
            }
        }
    }

    @Override
    public Mods.ModBase getMod() {
        return Mods.OpenComputers;
    }

    @Override
    public void initialize() {
        DroneTemplate.register();
        MicrocontrollerTemplate.register();
        NavigationUpgradeTemplate.register();
        RobotTemplate.register();
        ServerTemplate.register();
        TabletTemplate.register();
        TemplateBlacklist.register();

        JEI.hide(li.cil.oc.api.Items.get(Constants.BlockName.RobotAfterimage).block());
        JEI.hide(li.cil.oc.api.Items.get(Constants.BlockName.Print).block());
        JEI.hide(li.cil.oc.api.Items.get(Constants.BlockName.BeaconBasePrint).block());
        JEI.hide((DelegateItem) li.cil.oc.api.Items.get(Constants.ItemName.Present).item());

        li.cil.oc.core.impl.util.ComponentDriverHelper.setRedstoneCardCheck(d -> d instanceof DriverRedstoneCard);
        li.cil.oc.core.util.FluidTankHelper.setInstance(new li.cil.oc.neoforge.util.FluidTankHelper());
        li.cil.oc.core.impl.util.DroneHelper.setInstance(new li.cil.oc.neoforge.util.DroneHelper());
        li.cil.oc.core.impl.util.GeolyzerHostHelper.setInstance(new li.cil.oc.neoforge.util.GeolyzerHostHelper());
        li.cil.oc.core.impl.common.entity.Drone.setEntityType(li.cil.oc.neoforge.common.init.Entities.DRONE.get());
        li.cil.oc.core.impl.common.entity.Drone.setControlFactory(Drone::new);
        li.cil.oc.core.impl.common.entity.Drone.setMenuOpener(li.cil.oc.neoforge.util.DroneMenuOpener.INSTANCE);
        li.cil.oc.core.util.RobotChargeableFactory.setInstance(new li.cil.oc.neoforge.util.RobotChargeableFactory());

        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers.isWrench");
        li.cil.oc.core.impl.common.Registrar.registerItemCharge(
                "OpenComputers",
                "li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers.canCharge",
                "li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers.charge");

        li.cil.oc.core.impl.common.Registrar.registerInkProvider("li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers.inkCartridgeInkProvider");
        li.cil.oc.core.impl.common.Registrar.registerInkProvider("li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers.dyeInkProvider");

        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("build", "builder", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("dig", "dig", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("base64", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("deflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("gpg", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("inflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("md5sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("sha256sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("refuel", "generator", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("irc", "irc", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("maze", "maze", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("arp", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("ifconfig", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("ping", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("route", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("opl-flash", "openloader", "Lua 5.2", "Lua 5.3", "LuaJ");
        li.cil.oc.core.impl.common.Registrar.registerProgramDiskLabel("oppm", "oppm", "Lua 5.2", "Lua 5.3", "LuaJ");

        NeoForge.EVENT_BUS.register(EventHandler.class);
        NeoForge.EVENT_BUS.register(NanomachinesHandler.Common.class);
        NeoForge.EVENT_BUS.register(SimpleComponentTickHandler.Instance);

        NeoForge.EVENT_BUS.register(li.cil.oc.neoforge.common.event.AnalyzerEventHandler.class);
        NeoForge.EVENT_BUS.register(AngelUpgradeHandler.class);
        NeoForge.EVENT_BUS.register(BlockChangeHandler.class);
        NeoForge.EVENT_BUS.register(ChunkloaderUpgradeHandler.class);
        NeoForge.EVENT_BUS.register(EventHandler.class);
        NeoForge.EVENT_BUS.register(ExperienceUpgradeHandler.class);
        NeoForge.EVENT_BUS.register(FileSystemAccessHandler.class);
        NeoForge.EVENT_BUS.register(HoverBootsHandler.class);
        NeoForge.EVENT_BUS.register(Loot.class);
        NeoForge.EVENT_BUS.register(NanomachinesHandler.Common.class);
        NeoForge.EVENT_BUS.register(NetworkActivityHandler.class);
        NeoForge.EVENT_BUS.register(RobotCommonHandler.class);
        NeoForge.EVENT_BUS.register(SaveHandler.class);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> {
            var level = e.getLevel();
            if (level instanceof net.minecraft.world.level.Level l) {
                var cache = li.cil.oc.core.impl.util.TabletCache.get();
                if (cache != null) cache.saveAll(l);
            }
        });
        NeoForge.EVENT_BUS.register(Waypoints.class);
        NeoForge.EVENT_BUS.register(WirelessNetwork.class);
        NeoForge.EVENT_BUS.register(WirelessNetworkCardHandler.class);
        NeoForge.EVENT_BUS.register(li.cil.oc.neoforge.server.ComponentTracker.INSTANCE);

        li.cil.oc.api.Driver.add(new ConverterNanomachines());
        li.cil.oc.api.Driver.add(new ConverterLinkedCard());

        li.cil.oc.api.Driver.add(new DriverAPU());
        li.cil.oc.api.Driver.add(new DriverComponentBus());
        li.cil.oc.api.Driver.add(new DriverCPU());
        li.cil.oc.api.Driver.add(new DriverDataCard());
        li.cil.oc.api.Driver.add(new DriverDebugCard());
        li.cil.oc.api.Driver.add(new DriverEEPROM());
        li.cil.oc.api.Driver.add(new DriverFileSystem());
        li.cil.oc.api.Driver.add(new DriverGraphicsCard());
        li.cil.oc.api.Driver.add(new DriverInternetCard());
        li.cil.oc.api.Driver.add(new DriverLinkedCard());
        li.cil.oc.api.Driver.add(new DriverMemory());
        li.cil.oc.api.Driver.add(new DriverNetworkCard());
        li.cil.oc.api.Driver.add(new DriverKeyboard());
        li.cil.oc.api.Driver.add(new DriverRedstoneCard());
        li.cil.oc.api.Driver.add(new DriverTablet());
        li.cil.oc.api.Driver.add(new DriverWirelessNetworkCard());

        li.cil.oc.api.Driver.add(new DriverContainerCard());
        li.cil.oc.api.Driver.add(new DriverContainerFloppy());
        li.cil.oc.api.Driver.add(new DriverContainerUpgrade());

        li.cil.oc.api.Driver.add(new DriverGeolyzer());
        li.cil.oc.api.Driver.add(new DriverMotionSensor());
        li.cil.oc.api.Driver.add(new DriverScreen());
        li.cil.oc.api.Driver.add(new DriverTransposer());

        li.cil.oc.api.Driver.add(new DriverDiskDriveMountable());
        li.cil.oc.api.Driver.add(new DriverServer());
        li.cil.oc.api.Driver.add(new DriverTerminalServer());

        li.cil.oc.api.Driver.add(new DriverUpgradeAngel());
        li.cil.oc.api.Driver.add(new DriverUpgradeBarcodeReader());
        li.cil.oc.api.Driver.add(new DriverUpgradeBattery());
        li.cil.oc.api.Driver.add(new DriverUpgradeChunkloader());
        li.cil.oc.api.Driver.add(new DriverUpgradeCrafting());
        li.cil.oc.api.Driver.add(new DriverUpgradeDatabase());
        li.cil.oc.api.Driver.add(new DriverUpgradeExperience());
        li.cil.oc.api.Driver.add(new DriverUpgradeGenerator());
        li.cil.oc.api.Driver.add(new DriverUpgradeHover());
        li.cil.oc.api.Driver.add(new DriverUpgradeInventory());
        li.cil.oc.api.Driver.add(new DriverUpgradeInventoryController());
        li.cil.oc.api.Driver.add(new DriverUpgradeLeash());
        li.cil.oc.api.Driver.add(new DriverUpgradeNavigation());
        li.cil.oc.api.Driver.add(new DriverUpgradePiston());
        li.cil.oc.api.Driver.add(new DriverUpgradeSign());
        li.cil.oc.api.Driver.add(new DriverUpgradeSolarGenerator());
        li.cil.oc.api.Driver.add(new DriverUpgradeTank());
        li.cil.oc.api.Driver.add(new DriverUpgradeTankController());
        li.cil.oc.api.Driver.add(new DriverUpgradeTractorBeam());
        li.cil.oc.api.Driver.add(new DriverUpgradeTrading());
        li.cil.oc.api.Driver.add(new DriverUpgradeMF());
        li.cil.oc.api.Driver.add(new DriverAPU.Provider());
        li.cil.oc.api.Driver.add(new DriverDataCard.Provider());
        li.cil.oc.api.Driver.add(new DriverDebugCard.Provider());
        li.cil.oc.api.Driver.add(new DriverEEPROM.Provider());
        li.cil.oc.api.Driver.add(new DriverGraphicsCard.Provider());
        li.cil.oc.api.Driver.add(new DriverInternetCard.Provider());
        li.cil.oc.api.Driver.add(new DriverLinkedCard.Provider());
        li.cil.oc.api.Driver.add(new DriverNetworkCard.Provider());
        li.cil.oc.api.Driver.add(new DriverRedstoneCard.Provider());
        li.cil.oc.api.Driver.add(new DriverWirelessNetworkCard.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeDatabase.Provider());

        li.cil.oc.api.Driver.add(new DriverGeolyzer.Provider());
        li.cil.oc.api.Driver.add(new DriverMotionSensor.Provider());
        li.cil.oc.api.Driver.add(new DriverScreen.Provider());
        li.cil.oc.api.Driver.add(new DriverTransposer.Provider());

        li.cil.oc.api.Driver.add(new DriverUpgradeChunkloader.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeCrafting.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeDatabase.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeExperience.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeGenerator.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeInventoryController.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeLeash.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeNavigation.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradePiston.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeSign.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeTankController.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeTractorBeam.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeMF.Provider());

        li.cil.oc.api.Driver.add(new EnvironmentProviderBlocks());

        li.cil.oc.api.Driver.add(new InventoryProviderDatabase());
        li.cil.oc.api.Driver.add(new InventoryProviderServer());
        blacklistHost(li.cil.oc.api.internal.Adapter.class,
                Constants.BlockName.Geolyzer,
                Constants.BlockName.MotionSensor,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.BatteryUpgradeTier1,
                Constants.ItemName.BatteryUpgradeTier2,
                Constants.ItemName.BatteryUpgradeTier3,
                Constants.ItemName.ChunkloaderUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.NavigationUpgrade,
                Constants.ItemName.PistonUpgrade,
                Constants.ItemName.StickyPistonUpgrade,
                Constants.ItemName.SolarGeneratorUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TractorBeamUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);
        blacklistHost(li.cil.oc.api.internal.Drone.class,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.APUTier1,
                Constants.ItemName.APUTier2,
                Constants.ItemName.GraphicsCardTier1,
                Constants.ItemName.GraphicsCardTier2,
                Constants.ItemName.GraphicsCardTier3,
                Constants.ItemName.NetworkCard,
                Constants.ItemName.RedstoneCardTier1,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2);
        blacklistHost(li.cil.oc.api.internal.Microcontroller.class,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.APUTier1,
                Constants.ItemName.APUTier2,
                Constants.ItemName.GraphicsCardTier1,
                Constants.ItemName.GraphicsCardTier2,
                Constants.ItemName.GraphicsCardTier3,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.DatabaseUpgradeTier1,
                Constants.ItemName.DatabaseUpgradeTier2,
                Constants.ItemName.DatabaseUpgradeTier3,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.InventoryControllerUpgrade,
                Constants.ItemName.NavigationUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TankControllerUpgrade,
                Constants.ItemName.TractorBeamUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);
        blacklistHost(li.cil.oc.api.internal.Robot.class,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.LeashUpgrade);
        blacklistHost(li.cil.oc.api.internal.Tablet.class,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.NetworkCard,
                Constants.ItemName.RedstoneCardTier1,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.ChunkloaderUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.DatabaseUpgradeTier1,
                Constants.ItemName.DatabaseUpgradeTier2,
                Constants.ItemName.DatabaseUpgradeTier3,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.InventoryControllerUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TankControllerUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);

        if (!li.cil.oc.core.impl.integration.util.BundledRedstone.isAvailable() && !WirelessRedstone.isAvailable()) {
            blacklistHost(li.cil.oc.api.internal.Drone.class, Constants.ItemName.RedstoneCardTier2);
            blacklistHost(li.cil.oc.api.internal.Tablet.class, Constants.ItemName.RedstoneCardTier2);
        }

        li.cil.oc.api.Manual.addProvider(new DefinitionPathProvider());
        li.cil.oc.api.Manual.addProvider(new ResourceContentProvider(OCSettings.resourceDomain, "doc/"));
        li.cil.oc.api.Manual.addProvider("", new TextureImageProvider());
        li.cil.oc.api.Manual.addProvider("item", new ItemImageProvider());
        li.cil.oc.api.Manual.addProvider("block", new BlockImageProvider());
        li.cil.oc.api.Manual.addProvider("tag", new OreDictImageProvider());
        li.cil.oc.api.Manual.addTab(new li.cil.oc.api.prefab.TextureTabIconRenderer(li.cil.oc.core.impl.client.Textures.guiManualHome), "gui.opencomputers.manual.home", "%LANGUAGE%/index.md");
        li.cil.oc.api.Manual.addTab(new li.cil.oc.api.prefab.ItemStackTabIconRenderer(li.cil.oc.api.Items.get("case1").createItemStack(1)), "gui.opencomputers.manual.blocks", "%LANGUAGE%/block/index.md");

        li.cil.oc.api.Manual.addTab(new li.cil.oc.api.prefab.ItemStackTabIconRenderer(li.cil.oc.api.Items.get("cpu1").createItemStack(1)), "gui.opencomputers.manual.items", "%LANGUAGE%/item/index.md");

        li.cil.oc.api.Nanomachines.addProvider(new DisintegrationProvider());
        li.cil.oc.api.Nanomachines.addProvider(new HungryProvider());
        li.cil.oc.api.Nanomachines.addProvider(new ParticleProvider());
        li.cil.oc.api.Nanomachines.addProvider(new PotionProvider());
        li.cil.oc.api.Nanomachines.addProvider(new MagnetProvider());
    }

    public static final class DefinitionPathProvider implements PathProvider {
        private static final HashSet<String> Blacklist = new HashSet<>(Arrays.asList(
                Constants.ItemName.APUCreative,
                Constants.ItemName.Debugger,
                Constants.ItemName.DiamondChip,
                Constants.ItemName.Present,
                Constants.BlockName.CarpetedCapacitor,
                Constants.BlockName.Endstone,
                Constants.BlockName.RobotAfterimage));

        private static String checkBlacklisted(ItemInfo info) {
            if (info == null || Blacklist.contains(info.name())) {
                return null;
            }
            if (info.name().equals(Constants.BlockName.BeaconBasePrint)) {
                return "%LANGUAGE%/block/" + Constants.BlockName.Print + ".md";
            }
            if (info.block() != null) {
                return "%LANGUAGE%/block/" + info.name() + ".md";
            }
            return "%LANGUAGE%/item/" + info.name() + ".md";
        }

        @Override
        public String pathFor(ItemStack stack) {
            var info = li.cil.oc.api.Items.get(stack);
            return info != null ? checkBlacklisted(info) : null;
        }

        @Override
        public String pathFor(Level world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof SimpleBlock) {
                ItemInfo info = li.cil.oc.api.Items.get(new ItemStack(block));
                if (info != null) {
                    return checkBlacklisted(info);
                }
            }
            return null;
        }
    }
}
