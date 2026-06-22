package li.cil.oc.neoforge.common.init;

import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.HoverBoots;
import li.cil.oc.core.impl.common.item.data.DroneData;
import li.cil.oc.core.impl.common.item.data.HoverBootsData;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.Loot;
import li.cil.oc.neoforge.common.block.SimpleBlock;
import li.cil.oc.neoforge.common.item.ALU;
import li.cil.oc.neoforge.common.item.APU;
import li.cil.oc.neoforge.common.item.Acid;
import li.cil.oc.neoforge.common.item.Analyzer;
import li.cil.oc.neoforge.common.item.ArrowKeys;
import li.cil.oc.neoforge.common.item.ButtonGroup;
import li.cil.oc.neoforge.common.item.CPU;
import li.cil.oc.neoforge.common.item.CardBase;
import li.cil.oc.neoforge.common.item.Chamelium;
import li.cil.oc.neoforge.common.item.CircuitBoard;
import li.cil.oc.neoforge.common.item.ComponentBus;
import li.cil.oc.neoforge.common.item.ControlUnit;
import li.cil.oc.neoforge.common.item.CuttingWire;
import li.cil.oc.neoforge.common.item.DataCard;
import li.cil.oc.neoforge.common.item.DebugCard;
import li.cil.oc.neoforge.common.item.Debugger;
import li.cil.oc.neoforge.common.item.DiamondChip;
import li.cil.oc.neoforge.common.item.Disk;
import li.cil.oc.neoforge.common.item.DiskDriveMountable;
import li.cil.oc.neoforge.common.item.Drone;
import li.cil.oc.neoforge.common.item.DroneCase;
import li.cil.oc.neoforge.common.item.EEPROM;
import li.cil.oc.neoforge.common.item.FloppyDisk;
import li.cil.oc.neoforge.common.item.GraphicsCard;
import li.cil.oc.neoforge.common.item.HardDiskDrive;
import li.cil.oc.neoforge.common.item.InkCartridge;
import li.cil.oc.neoforge.common.item.InkCartridgeEmpty;
import li.cil.oc.neoforge.common.item.InternetCard;
import li.cil.oc.neoforge.common.item.Interweb;
import li.cil.oc.neoforge.common.item.LinkedCard;
import li.cil.oc.neoforge.common.item.Manual;
import li.cil.oc.neoforge.common.item.Memory;
import li.cil.oc.neoforge.common.item.Microchip;
import li.cil.oc.neoforge.common.item.MicrocontrollerCase;
import li.cil.oc.neoforge.common.item.Nanomachines;
import li.cil.oc.neoforge.common.item.NetworkCard;
import li.cil.oc.neoforge.common.item.NumPad;
import li.cil.oc.neoforge.common.item.Present;
import li.cil.oc.neoforge.common.item.PrintedCircuitBoard;
import li.cil.oc.neoforge.common.item.RawCircuitBoard;
import li.cil.oc.neoforge.common.item.RedstoneCard;
import li.cil.oc.neoforge.common.item.Server;
import li.cil.oc.neoforge.common.item.Tablet;
import li.cil.oc.neoforge.common.item.TabletCase;
import li.cil.oc.neoforge.common.item.Terminal;
import li.cil.oc.neoforge.common.item.TerminalServer;
import li.cil.oc.neoforge.common.item.TexturePicker;
import li.cil.oc.neoforge.common.item.TpsCard;
import li.cil.oc.neoforge.common.item.Transistor;
import li.cil.oc.neoforge.common.item.UpgradeAngel;
import li.cil.oc.neoforge.common.item.UpgradeBattery;
import li.cil.oc.neoforge.common.item.UpgradeChunkloader;
import li.cil.oc.neoforge.common.item.UpgradeContainerCard;
import li.cil.oc.neoforge.common.item.UpgradeContainerUpgrade;
import li.cil.oc.neoforge.common.item.UpgradeCrafting;
import li.cil.oc.neoforge.common.item.UpgradeDatabase;
import li.cil.oc.neoforge.common.item.UpgradeExperience;
import li.cil.oc.neoforge.common.item.UpgradeGenerator;
import li.cil.oc.neoforge.common.item.UpgradeHover;
import li.cil.oc.neoforge.common.item.UpgradeInventory;
import li.cil.oc.neoforge.common.item.UpgradeInventoryController;
import li.cil.oc.neoforge.common.item.UpgradeLeash;
import li.cil.oc.neoforge.common.item.UpgradeMF;
import li.cil.oc.neoforge.common.item.UpgradeNavigation;
import li.cil.oc.neoforge.common.item.UpgradePiston;
import li.cil.oc.neoforge.common.item.UpgradeRITEG;
import li.cil.oc.neoforge.common.item.UpgradeSign;
import li.cil.oc.neoforge.common.item.UpgradeSolarGenerator;
import li.cil.oc.neoforge.common.item.UpgradeStickyPiston;
import li.cil.oc.neoforge.common.item.UpgradeTank;
import li.cil.oc.neoforge.common.item.UpgradeTankController;
import li.cil.oc.neoforge.common.item.UpgradeTractorBeam;
import li.cil.oc.neoforge.common.item.UpgradeTrading;
import li.cil.oc.neoforge.common.item.WirelessNetworkCard;
import li.cil.oc.neoforge.common.item.Wrench;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

public final class Items implements ItemAPI {
    private static final Item.Properties P = new Item.Properties();
    private static final Item.Properties P1 = new Item.Properties().stacksTo(1);
    public static final Map<String, ItemInfo> descriptors = new HashMap<>();
    public static final Map<Object, String> names = new HashMap<>();

    public static final Map<String, String> aliases = new HashMap<>();
    public static final List<ItemStack> registeredItems = new ArrayList<>();

    static {
        aliases.put("dataCard", Constants.ItemName.DataCardTier1);
        aliases.put("wlanCard", Constants.ItemName.WirelessNetworkCardTier2);
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, OpenComputers.ID);

    private static <B extends Block> DeferredHolder<Item, BlockItem> registerBlockItem(
            String name, DeferredHolder<Block, B> block) {
        return ITEMS.register(name.toLowerCase(Locale.ROOT), () -> new li.cil.oc.neoforge.common.block.Item(block.get()));
    }

    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> ADAPTER = registerBlockItem(Constants.BlockName.Adapter, Blocks.ADAPTER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> ASSEMBLER = registerBlockItem(Constants.BlockName.Assembler, Blocks.ASSEMBLER);
    public static final DeferredHolder<Item, BlockItem> CABLE = ITEMS.register(Constants.BlockName.Cable.toLowerCase(Locale.ROOT), () -> new li.cil.oc.neoforge.common.block.CableItem(Blocks.CABLE.get()));
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CAPACITOR = registerBlockItem(Constants.BlockName.Capacitor, Blocks.CAPACITOR);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CASE_TIER_1 = registerBlockItem(Constants.BlockName.CaseTier1, Blocks.CASE_TIER_1);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CASE_TIER_2 = registerBlockItem(Constants.BlockName.CaseTier2, Blocks.CASE_TIER_2);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CASE_TIER_3 = registerBlockItem(Constants.BlockName.CaseTier3, Blocks.CASE_TIER_3);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CHARGER = registerBlockItem(Constants.BlockName.Charger, Blocks.CHARGER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> DISASSEMBLER = registerBlockItem(Constants.BlockName.Disassembler, Blocks.DISASSEMBLER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> DISK_DRIVE = registerBlockItem(Constants.BlockName.DiskDrive, Blocks.DISK_DRIVE);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> GEOLYZER = registerBlockItem(Constants.BlockName.Geolyzer, Blocks.GEOLYZER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> HOLOGRAM_TIER_1 = registerBlockItem(Constants.BlockName.HologramTier1, Blocks.HOLOGRAM_TIER_1);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> HOLOGRAM_TIER_2 = registerBlockItem(Constants.BlockName.HologramTier2, Blocks.HOLOGRAM_TIER_2);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> KEYBOARD = registerBlockItem(Constants.BlockName.Keyboard, Blocks.KEYBOARD);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> MOTION_SENSOR = registerBlockItem(Constants.BlockName.MotionSensor, Blocks.MOTION_SENSOR);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> POWER_CONVERTER = registerBlockItem(Constants.BlockName.PowerConverter, Blocks.POWER_CONVERTER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> POWER_DISTRIBUTOR = registerBlockItem(Constants.BlockName.PowerDistributor, Blocks.POWER_DISTRIBUTOR);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> RAID = registerBlockItem(Constants.BlockName.Raid, Blocks.RAID);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> REDSTONE = registerBlockItem(Constants.BlockName.Redstone, Blocks.REDSTONE);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> RELAY = registerBlockItem(Constants.BlockName.Relay, Blocks.RELAY);
    public static final DeferredHolder<Item, BlockItem> SCREEN_TIER_1 = registerBlockItem(Constants.BlockName.ScreenTier1, Blocks.SCREEN_TIER_1);
    public static final DeferredHolder<Item, BlockItem> SCREEN_TIER_2 = registerBlockItem(Constants.BlockName.ScreenTier2, Blocks.SCREEN_TIER_2);
    public static final DeferredHolder<Item, BlockItem> SCREEN_TIER_3 = registerBlockItem(Constants.BlockName.ScreenTier3, Blocks.SCREEN_TIER_3);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> RACK = registerBlockItem(Constants.BlockName.Rack, Blocks.RACK);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CASE_CREATIVE = registerBlockItem(Constants.BlockName.CaseCreative, Blocks.CASE_CREATIVE);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> MICROCONTROLLER = registerBlockItem(Constants.BlockName.Microcontroller, Blocks.MICROCONTROLLER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> ROBOT_AFTERIMAGE = registerBlockItem(Constants.BlockName.RobotAfterimage, Blocks.ROBOT_AFTERIMAGE);
    public static final DeferredHolder<Item, BlockItem> ROBOT = registerBlockItem(Constants.BlockName.Robot, Blocks.ROBOT);
    public static final DeferredHolder<Item, BlockItem> PRINT = registerBlockItem("print", Blocks.PRINT);
    public static final DeferredHolder<Item, BlockItem> BEACON_BASE_PRINT = registerBlockItem(Constants.BlockName.BeaconBasePrint, Blocks.BEACON_BASE_PRINT);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> PRINTER = registerBlockItem("printer", Blocks.PRINTER);
    public static final DeferredHolder<Item, BlockItem> CHAMELIUM_BLOCK = ITEMS.register("chameliumblock", () -> new li.cil.oc.neoforge.common.block.ChameliumBlockItem(Blocks.CHAMELIUM_BLOCK.get()));
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> WAYPOINT = registerBlockItem(Constants.BlockName.Waypoint, Blocks.WAYPOINT);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> ENDSTONE = registerBlockItem(Constants.BlockName.Endstone, Blocks.ENDSTONE);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> NET_SPLITTER = registerBlockItem(Constants.BlockName.NetSplitter, Blocks.NET_SPLITTER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> TRANSPOSER = registerBlockItem(Constants.BlockName.Transposer, Blocks.TRANSPOSER);
    @SuppressWarnings("unused")
    public static final DeferredHolder<Item, BlockItem> CARPETED_CAPACITOR = registerBlockItem(Constants.BlockName.CarpetedCapacitor, Blocks.CARPETED_CAPACITOR);

    public static final DeferredHolder<Item, ALU> ALU = reg("alu", () -> new ALU(P));
    public static final DeferredHolder<Item, Acid> ACID = reg("acid", () -> new Acid(P));
    public static final DeferredHolder<Item, ArrowKeys> ARROW_KEYS = reg("arrowkeys", () -> new ArrowKeys(P));
    public static final DeferredHolder<Item, ButtonGroup> BUTTON_GROUP = reg("buttongroup", () -> new ButtonGroup(P));
    public static final DeferredHolder<Item, CardBase> CARD_BASE = reg("card", () -> new CardBase(P));
    public static final DeferredHolder<Item, Chamelium> CHAMELIUM = reg("chamelium", () -> new Chamelium(P));
    public static final DeferredHolder<Item, CircuitBoard> CIRCUIT_BOARD = reg("circuitboard", () -> new CircuitBoard(P));
    public static final DeferredHolder<Item, ControlUnit> CONTROL_UNIT = reg("cu", () -> new ControlUnit(P));
    public static final DeferredHolder<Item, CuttingWire> CUTTING_WIRE = reg("cuttingwire", () -> new CuttingWire(P));
    public static final DeferredHolder<Item, DiamondChip> DIAMOND_CHIP = reg("chipdiamond", () -> new DiamondChip(P));
    public static final DeferredHolder<Item, Disk> DISK = reg("disk", () -> new Disk(P));
    public static final DeferredHolder<Item, Interweb> INTERWEB = reg("interweb", () -> new Interweb(P));
    public static final DeferredHolder<Item, Microchip> MICROCHIP_T1 = reg("chip1", () -> new Microchip(P, Tier.One));
    public static final DeferredHolder<Item, Microchip> MICROCHIP_T2 = reg("chip2", () -> new Microchip(P, Tier.Two));
    public static final DeferredHolder<Item, Microchip> MICROCHIP_T3 = reg("chip3", () -> new Microchip(P, Tier.Three));
    public static final DeferredHolder<Item, NumPad> NUM_PAD = reg("numpad", () -> new NumPad(P));
    public static final DeferredHolder<Item, PrintedCircuitBoard> PRINTED_CIRCUIT_BOARD = reg("printedcircuitboard", () -> new PrintedCircuitBoard(P));
    public static final DeferredHolder<Item, RawCircuitBoard> RAW_CIRCUIT_BOARD = reg("rawcircuitboard", () -> new RawCircuitBoard(P));
    public static final DeferredHolder<Item, Transistor> TRANSISTOR = reg("transistor", () -> new Transistor(P));


    public static final DeferredHolder<Item, Memory> RAM_T1 = reg("ram1", () -> new Memory(P, Tier.One));
    public static final DeferredHolder<Item, Memory> RAM_T2 = reg("ram2", () -> new Memory(P, Tier.Two));
    public static final DeferredHolder<Item, Memory> RAM_T3 = reg("ram3", () -> new Memory(P, Tier.Three));
    public static final DeferredHolder<Item, Memory> RAM_T4 = reg("ram4", () -> new Memory(P, Tier.Four));
    public static final DeferredHolder<Item, Memory> RAM_T5 = reg("ram5", () -> new Memory(P, Tier.Five));
    public static final DeferredHolder<Item, Memory> RAM_T6 = reg("ram6", () -> new Memory(P, Tier.Six));

    
    public static final DeferredHolder<Item, HardDiskDrive> HDD_T1 = reg("hdd1", () -> new HardDiskDrive(P, Tier.One));
    public static final DeferredHolder<Item, HardDiskDrive> HDD_T2 = reg("hdd2", () -> new HardDiskDrive(P, Tier.Two));
    public static final DeferredHolder<Item, HardDiskDrive> HDD_T3 = reg("hdd3", () -> new HardDiskDrive(P, Tier.Three));

    
    public static final DeferredHolder<Item, CPU> CPU_T1 = reg("cpu1", () -> new CPU(P, Tier.One));
    public static final DeferredHolder<Item, CPU> CPU_T2 = reg("cpu2", () -> new CPU(P, Tier.Two));
    public static final DeferredHolder<Item, CPU> CPU_T3 = reg("cpu3", () -> new CPU(P, Tier.Three));

    
    public static final DeferredHolder<Item, GraphicsCard> GRAPHICS_CARD_T1 = reg("graphicscard1", () -> new GraphicsCard(P, Tier.One));
    public static final DeferredHolder<Item, GraphicsCard> GRAPHICS_CARD_T2 = reg("graphicscard2", () -> new GraphicsCard(P, Tier.Two));
    public static final DeferredHolder<Item, GraphicsCard> GRAPHICS_CARD_T3 = reg("graphicscard3", () -> new GraphicsCard(P, Tier.Three));

    
    public static final DeferredHolder<Item, APU> APU_T1 = reg("apu1", () -> new APU(P, Tier.One));
    public static final DeferredHolder<Item, APU> APU_T2 = reg("apu2", () -> new APU(P, Tier.Two));
    public static final DeferredHolder<Item, APU> APU_CREATIVE = reg("apucreative", () -> new APU(P, Tier.Three));

    
    public static final DeferredHolder<Item, ComponentBus> COMPONENT_BUS_T1 = reg("componentbus1", () -> new ComponentBus(P, Tier.One));
    public static final DeferredHolder<Item, ComponentBus> COMPONENT_BUS_T2 = reg("componentbus2", () -> new ComponentBus(P, Tier.Two));
    public static final DeferredHolder<Item, ComponentBus> COMPONENT_BUS_T3 = reg("componentbus3", () -> new ComponentBus(P, Tier.Three));
    public static final DeferredHolder<Item, ComponentBus> COMPONENT_BUS_CREATIVE = reg("componentbuscreative", () -> new ComponentBus(P, Tier.Four));

    
    public static final DeferredHolder<Item, NetworkCard> NETWORK_CARD = reg("lancard", () -> new NetworkCard(P));
    public static final DeferredHolder<Item, WirelessNetworkCard> WIRELESS_CARD_T1 = reg("wlancard1", () -> new WirelessNetworkCard(P, Tier.One));
    public static final DeferredHolder<Item, WirelessNetworkCard> WIRELESS_CARD_T2 = reg("wlancard2", () -> new WirelessNetworkCard(P, Tier.Two));

    
    public static final DeferredHolder<Item, RedstoneCard> REDSTONE_CARD_T1 = reg("redstonecard1", () -> new RedstoneCard(P, Tier.One));
    public static final DeferredHolder<Item, RedstoneCard> REDSTONE_CARD_T2 = reg("redstonecard2", () -> new RedstoneCard(P, Tier.Two));

    
    public static final DeferredHolder<Item, DataCard> DATA_CARD_T1 = reg("datacard1", () -> new DataCard(P, Tier.One));
    public static final DeferredHolder<Item, DataCard> DATA_CARD_T2 = reg("datacard2", () -> new DataCard(P, Tier.Two));
    public static final DeferredHolder<Item, DataCard> DATA_CARD_T3 = reg("datacard3", () -> new DataCard(P, Tier.Three));

    
    public static final DeferredHolder<Item, Analyzer> ANALYZER = reg("analyzer", () -> new Analyzer(P));
    public static final DeferredHolder<Item, DebugCard> DEBUG_CARD = reg("debugcard", () -> new DebugCard(P));
    public static final DeferredHolder<Item, Debugger> DEBUGGER = reg("debugger", () -> new Debugger(P));
    public static final DeferredHolder<Item, InternetCard> INTERNET_CARD = reg("internetcard", () -> new InternetCard(P));
    public static final DeferredHolder<Item, LinkedCard> LINKED_CARD = reg("linkedcard", () -> new LinkedCard(P));
    public static final DeferredHolder<Item, Manual> MANUAL = reg("manual", () -> new Manual(P));
    public static final DeferredHolder<Item, Nanomachines> NANOMACHINES = reg("nanomachines", () -> new Nanomachines(P));
    public static final DeferredHolder<Item, TexturePicker> TEXTURE_PICKER = reg("texturepicker", () -> new TexturePicker(P));
    public static final DeferredHolder<Item, TpsCard> TPS_CARD = reg("tpscard", () -> new TpsCard(P));
    public static final DeferredHolder<Item, Wrench> WRENCH = reg("wrench", () -> new Wrench(P));

    
    public static final DeferredHolder<Item, FloppyDisk> FLOPPY = reg("floppy", () -> new FloppyDisk(P));
    public static final DeferredHolder<Item, Terminal> TERMINAL = reg("terminal", () -> new Terminal(P));
    public static final DeferredHolder<Item, TerminalServer> TERMINAL_SERVER = reg("terminalserver", () -> new TerminalServer(P));
    public static final DeferredHolder<Item, DiskDriveMountable> DISK_DRIVE_MOUNTABLE = reg("diskdrivemountable", () -> new DiskDriveMountable(P));

    
    public static final DeferredHolder<Item, InkCartridgeEmpty> INK_CARTRIDGE_EMPTY = reg("inkcartridgeempty", () -> new InkCartridgeEmpty(P1));
    public static final DeferredHolder<Item, InkCartridge> INK_CARTRIDGE = reg("inkcartridge", () -> new InkCartridge(P));

    
    public static final DeferredHolder<Item, Server> SERVER_T1 = reg("server1", () -> new Server(P1, Tier.One));
    public static final DeferredHolder<Item, Server> SERVER_T2 = reg("server2", () -> new Server(P1, Tier.Two));
    public static final DeferredHolder<Item, Server> SERVER_T3 = reg("server3", () -> new Server(P1, Tier.Three));
    public static final DeferredHolder<Item, Server> SERVER_CREATIVE = reg("servercreative", () -> new Server(P1, Tier.Four));

    
    public static final DeferredHolder<Item, Tablet> TABLET = reg("tablet", () -> new Tablet(P1));
    public static final DeferredHolder<Item, TabletCase> TABLET_CASE_T1 = reg("tabletcase1", () -> new TabletCase(P, Tier.One));
    public static final DeferredHolder<Item, TabletCase> TABLET_CASE_T2 = reg("tabletcase2", () -> new TabletCase(P, Tier.Two));
    public static final DeferredHolder<Item, TabletCase> TABLET_CASE_CREATIVE = reg("tabletcasecreative", () -> new TabletCase(P, Tier.Four));

    
    public static final DeferredHolder<Item, MicrocontrollerCase> MC_CASE_T1 = reg("microcontrollercase1", () -> new MicrocontrollerCase(P, Tier.One));
    public static final DeferredHolder<Item, MicrocontrollerCase> MC_CASE_T2 = reg("microcontrollercase2", () -> new MicrocontrollerCase(P, Tier.Two));
    public static final DeferredHolder<Item, MicrocontrollerCase> MC_CASE_CREATIVE = reg("microcontrollercasecreative", () -> new MicrocontrollerCase(P, Tier.Four));
    public static final DeferredHolder<Item, DroneCase> DRONE_CASE_T1 = reg("dronecase1", () -> new DroneCase(P, Tier.One));
    public static final DeferredHolder<Item, DroneCase> DRONE_CASE_T2 = reg("dronecase2", () -> new DroneCase(P, Tier.Two));
    public static final DeferredHolder<Item, DroneCase> DRONE_CASE_CREATIVE = reg("dronecasecreative", () -> new DroneCase(P, Tier.Four));
    public static final DeferredHolder<Item, Drone> DRONE = reg("drone", () -> new Drone(P));

    
    public static final DeferredHolder<Item, Present> PRESENT = reg("present", () -> new Present(P));

    
    public static final DeferredHolder<Item, UpgradeAngel> UPGRADE_ANGEL = reg("angelupgrade", () -> new UpgradeAngel(P));
    public static final DeferredHolder<Item, UpgradeCrafting> UPGRADE_CRAFTING = reg("craftingupgrade", () -> new UpgradeCrafting(P));
    public static final DeferredHolder<Item, UpgradeGenerator> UPGRADE_GENERATOR = reg("generatorupgrade", () -> new UpgradeGenerator(P));
    public static final DeferredHolder<Item, UpgradeSolarGenerator> UPGRADE_SOLAR = reg("solargeneratorupgrade", () -> new UpgradeSolarGenerator(P));
    public static final DeferredHolder<Item, UpgradeSign> UPGRADE_SIGN = reg("signupgrade", () -> new UpgradeSign(P));
    public static final DeferredHolder<Item, UpgradeNavigation> UPGRADE_NAV = reg("navigationupgrade", () -> new UpgradeNavigation(P));

    
    public static final DeferredHolder<Item, UpgradeBattery> BATTERY_UPGRADE_T1 = reg("batteryupgrade1", () -> new UpgradeBattery(P, Tier.One));
    public static final DeferredHolder<Item, UpgradeBattery> BATTERY_UPGRADE_T2 = reg("batteryupgrade2", () -> new UpgradeBattery(P, Tier.Two));
    public static final DeferredHolder<Item, UpgradeBattery> BATTERY_UPGRADE_T3 = reg("batteryupgrade3", () -> new UpgradeBattery(P, Tier.Three));

    
    public static final DeferredHolder<Item, UpgradeHover> HOVER_UPGRADE_T1 = reg("hoverupgrade1", () -> new UpgradeHover(P, Tier.One));
    public static final DeferredHolder<Item, UpgradeHover> HOVER_UPGRADE_T2 = reg("hoverupgrade2", () -> new UpgradeHover(P, Tier.Two));

    
    public static final DeferredHolder<Item, UpgradeContainerUpgrade> UPGRADE_CONTAINER_T1 = reg("upgradecontainer1", () -> new UpgradeContainerUpgrade(P, Tier.One));
    public static final DeferredHolder<Item, UpgradeContainerUpgrade> UPGRADE_CONTAINER_T2 = reg("upgradecontainer2", () -> new UpgradeContainerUpgrade(P, Tier.Two));
    public static final DeferredHolder<Item, UpgradeContainerUpgrade> UPGRADE_CONTAINER_T3 = reg("upgradecontainer3", () -> new UpgradeContainerUpgrade(P, Tier.Three));
    public static final DeferredHolder<Item, UpgradeContainerCard> CARD_CONTAINER_T1 = reg("cardcontainer1", () -> new UpgradeContainerCard(P, Tier.One));
    public static final DeferredHolder<Item, UpgradeContainerCard> CARD_CONTAINER_T2 = reg("cardcontainer2", () -> new UpgradeContainerCard(P, Tier.Two));
    public static final DeferredHolder<Item, UpgradeContainerCard> CARD_CONTAINER_T3 = reg("cardcontainer3", () -> new UpgradeContainerCard(P, Tier.Three));

    
    public static final DeferredHolder<Item, UpgradeDatabase> DATABASE_UPGRADE_T1 = reg("databaseupgrade1", () -> new UpgradeDatabase(P, Tier.One));
    public static final DeferredHolder<Item, UpgradeDatabase> DATABASE_UPGRADE_T2 = reg("databaseupgrade2", () -> new UpgradeDatabase(P, Tier.Two));
    public static final DeferredHolder<Item, UpgradeDatabase> DATABASE_UPGRADE_T3 = reg("databaseupgrade3", () -> new UpgradeDatabase(P, Tier.Three));

    
    public static final DeferredHolder<Item, UpgradeChunkloader> UPGRADE_CHUNKLOADER = reg("chunkloaderupgrade", () -> new UpgradeChunkloader(P));
    public static final DeferredHolder<Item, UpgradeExperience> UPGRADE_EXPERIENCE = reg("experienceupgrade", () -> new UpgradeExperience(P));
    public static final DeferredHolder<Item, UpgradeInventory> UPGRADE_INVENTORY = reg("inventoryupgrade", () -> new UpgradeInventory(P));
    public static final DeferredHolder<Item, UpgradeInventoryController> UPGRADE_INVENTORY_CTRL = reg("inventorycontrollerupgrade", () -> new UpgradeInventoryController(P));
    public static final DeferredHolder<Item, UpgradeLeash> UPGRADE_LEASH = reg("leashupgrade", () -> new UpgradeLeash(P));
    public static final DeferredHolder<Item, UpgradeMF> UPGRADE_MF = reg("mfu", () -> new UpgradeMF(P));
    public static final DeferredHolder<Item, UpgradePiston> UPGRADE_PISTON = reg("pistonupgrade", () -> new UpgradePiston(P));
    public static final DeferredHolder<Item, UpgradePiston> UPGRADE_STICKY_PISTON = reg("stickypistonupgrade", () -> new UpgradeStickyPiston(P));
    public static final DeferredHolder<Item, UpgradeRITEG> UPGRADE_RITEG = reg("upgraderiteg", () -> new UpgradeRITEG(P));
    public static final DeferredHolder<Item, UpgradeTank> UPGRADE_TANK = reg("tankupgrade", () -> new UpgradeTank(P));
    public static final DeferredHolder<Item, UpgradeTankController> UPGRADE_TANK_CTRL = reg("tankcontrollerupgrade", () -> new UpgradeTankController(P));
    public static final DeferredHolder<Item, UpgradeTractorBeam> UPGRADE_TRACTOR = reg("tractorbeamupgrade", () -> new UpgradeTractorBeam(P));
    public static final DeferredHolder<Item, UpgradeTrading> UPGRADE_TRADING = reg("tradingupgrade", () -> new UpgradeTrading(P));

    
    public static final DeferredHolder<Item, EEPROM> EEPROM_ITEM = reg("eeprom", EEPROM::new);
    public static final DeferredHolder<Item, HoverBoots> HOVER_BOOTS = reg("hoverboots", () -> new HoverBoots(P) {
        @Override
        protected java.util.List<net.minecraft.network.chat.Component> getExtendedTooltip(net.minecraft.world.item.ItemStack stack) {
            return Tooltip.get("hoverboots");
        }
    });

    private static <T extends Item> DeferredHolder<Item, T> reg(String name, java.util.function.Supplier<T> factory) {
        return ITEMS.register(name, factory);
    }

    public static final Items INSTANCE = new Items();

    private Items() {
    }

    public ItemInfo get(String name) {
        return descriptors.get(name);
    }

    public ItemInfo get(ItemStack stack) {
        String name = names.get(getBlockOrItem(stack));
        return name != null ? get(name) : null;
    }

    public static <T extends Block> void registerBlock(T instance, String id) {
        descriptors.put(id, new ItemInfo() {
            @Override
            public String name() {
                return id;
            }

            @Override
            public Block block() {
                return instance;
            }

            @Override
            public Item item() {
                return null;
            }

            @Override
            public ItemStack createItemStack(int size) {
                if (instance instanceof SimpleBlock) return ((SimpleBlock) instance).createItemStack(size);
                return new ItemStack(instance, size);
            }
        });
        names.put(instance, id);
    }

    public static void registerStack(ItemStack stack, String id) {
        ItemStack immutableStack = stack.copy();
        descriptors.put(id, new ItemInfo() {
            @Override
            public String name() {
                return id;
            }

            @Override
            public Block block() {
                return null;
            }

            @Override
            public ItemStack createItemStack(int size) {
                ItemStack copy = immutableStack.copy();
                copy.setCount(size);
                return copy;
            }

            @Override
            public Item item() {
                return immutableStack.getItem();
            }
        });
    }

    private static Object getBlockOrItem(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItem() instanceof BlockItem) return ((BlockItem) stack.getItem()).getBlock();
        return stack.getItem();
    }

    private static ItemStack safeGetStack(String name) {
        ItemInfo info = API.items.get(name);
        return info != null ? info.createItemStack(1) : null;
    }

    public static ItemStack createConfiguredDrone() {
        DroneData data = new DroneData();
        data.name = "Crecopter";
        data.tier = Tier.Four;
        data.storedEnergy = (int) Settings.get().bufferDrone;
        data.components = Arrays.asList(
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
                safeGetStack(Constants.ItemName.TankUpgrade),
                safeGetStack(Constants.ItemName.TankControllerUpgrade),
                safeGetStack(Constants.ItemName.LeashUpgrade),
                safeGetStack(Constants.ItemName.AngelUpgrade),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.RAMTier6)
        );
        return data.createItemStack();
    }

    public static ItemStack createConfiguredMicrocontroller() {
        MicrocontrollerData data = new MicrocontrollerData();
        data.tier = Tier.Four;
        data.storedEnergy = (int) Settings.get().bufferMicrocontroller;
        data.components = Arrays.asList(
                safeGetStack(Constants.ItemName.SignUpgrade),
                safeGetStack(Constants.ItemName.PistonUpgrade),
                safeGetStack(Constants.ItemName.RedstoneCardTier2),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.RAMTier6)
        );
        return data.createItemStack();
    }

    public static ItemStack createConfiguredRobot() {
        RobotData data = new RobotData();
        data.name = "Creatix";
        data.tier = Tier.Four;
        data.robotEnergy = (int) Settings.get().bufferRobot;
        data.totalEnergy = data.robotEnergy;
        data.components = Arrays.asList(
                safeGetStack(Constants.BlockName.ScreenTier1), safeGetStack(Constants.BlockName.Keyboard),
                safeGetStack(Constants.BlockName.Geolyzer), safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade), safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade), safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
                safeGetStack(Constants.ItemName.TankUpgrade), safeGetStack(Constants.ItemName.TankControllerUpgrade),
                safeGetStack(Constants.ItemName.CraftingUpgrade), safeGetStack(Constants.ItemName.HoverUpgradeTier2),
                safeGetStack(Constants.ItemName.AngelUpgrade), safeGetStack(Constants.ItemName.TradingUpgrade), safeGetStack(Constants.ItemName.ExperienceUpgrade),
                safeGetStack(Constants.ItemName.GraphicsCardTier3), safeGetStack(Constants.ItemName.RedstoneCardTier2),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2), safeGetStack(Constants.ItemName.InternetCard),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6), safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.LuaBios), safeGetStack(Constants.ItemName.OpenOS),
                safeGetStack(Constants.ItemName.HDDTier3)
        );
        data.containers = Arrays.asList(
                safeGetStack(Constants.ItemName.CardContainerTier3),
                safeGetStack(Constants.ItemName.UpgradeContainerTier3),
                safeGetStack(Constants.BlockName.DiskDrive)
        );
        return data.createItemStack();
    }

    public static ItemStack createConfiguredTablet() {
        TabletData data = new TabletData();
        data.tier = Tier.Four;
        data.energy = Settings.get().bufferTablet;
        data.maxEnergy = data.energy;
        List<ItemStack> items = new ArrayList<>(32);
        items.add(safeGetStack(Constants.BlockName.ScreenTier1));
        items.add(safeGetStack(Constants.BlockName.Keyboard));
        items.add(safeGetStack(Constants.ItemName.SignUpgrade));
        items.add(safeGetStack(Constants.ItemName.PistonUpgrade));
        items.add(safeGetStack(Constants.BlockName.Geolyzer));
        items.add(safeGetStack(Constants.ItemName.NavigationUpgrade));
        items.add(safeGetStack(Constants.ItemName.Analyzer));
        items.add(safeGetStack(Constants.ItemName.GraphicsCardTier2));
        items.add(safeGetStack(Constants.ItemName.RedstoneCardTier2));
        items.add(safeGetStack(Constants.ItemName.WirelessNetworkCardTier2));
        items.add(LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)));
        items.add(safeGetStack(Constants.ItemName.RAMTier6));
        items.add(safeGetStack(Constants.ItemName.RAMTier6));
        items.add(safeGetStack(Constants.ItemName.LuaBios));
        items.add(safeGetStack(Constants.ItemName.HDDTier3));
        while (items.size() < 32) items.add(null);
        items.set(31, safeGetStack(Constants.ItemName.OpenOS));
        data.items = items;
        data.container = safeGetStack(Constants.BlockName.DiskDrive);
        return data.createItemStack();
    }

    public static ItemStack createChargedHoverBoots() {
        HoverBootsData data = new HoverBootsData();
        data.charge = Settings.get().bufferHoverBoots;
        return data.createItemStack();
    }

    public static void init() {
        regItem(ALU, Constants.ItemName.Alu);
        regItem(ACID, Constants.ItemName.Acid);
        regItem(ARROW_KEYS, Constants.ItemName.ArrowKeys);
        regItem(BUTTON_GROUP, Constants.ItemName.ButtonGroup);
        regItem(CARD_BASE, Constants.ItemName.Card);
        regItem(CHAMELIUM, Constants.ItemName.Chamelium);
        regItem(CIRCUIT_BOARD, Constants.ItemName.CircuitBoard);
        regItem(CONTROL_UNIT, Constants.ItemName.ControlUnit);
        regItem(CUTTING_WIRE, Constants.ItemName.CuttingWire);
        regItem(DIAMOND_CHIP, Constants.ItemName.DiamondChip);
        regItem(DISK, Constants.ItemName.Disk);
        regItem(INTERWEB, Constants.ItemName.Interweb);
        regItem(MICROCHIP_T1, Constants.ItemName.ChipTier1);
        regItem(MICROCHIP_T2, Constants.ItemName.ChipTier2);
        regItem(MICROCHIP_T3, Constants.ItemName.ChipTier3);
        regItem(NUM_PAD, Constants.ItemName.NumPad);
        regItem(PRINTED_CIRCUIT_BOARD, Constants.ItemName.PrintedCircuitBoard);
        regItem(RAW_CIRCUIT_BOARD, Constants.ItemName.RawCircuitBoard);
        regItem(TRANSISTOR, Constants.ItemName.Transistor);

        regItem(RAM_T1, Constants.ItemName.RAMTier1);
        regItem(RAM_T2, Constants.ItemName.RAMTier2);
        regItem(RAM_T3, Constants.ItemName.RAMTier3);
        regItem(RAM_T4, Constants.ItemName.RAMTier4);
        regItem(RAM_T5, Constants.ItemName.RAMTier5);
        regItem(RAM_T6, Constants.ItemName.RAMTier6);

        regItem(HDD_T1, Constants.ItemName.HDDTier1);
        regItem(HDD_T2, Constants.ItemName.HDDTier2);
        regItem(HDD_T3, Constants.ItemName.HDDTier3);

        regItem(CPU_T1, Constants.ItemName.CPUTier1);
        regItem(CPU_T2, Constants.ItemName.CPUTier2);
        regItem(CPU_T3, Constants.ItemName.CPUTier3);

        regItem(GRAPHICS_CARD_T1, Constants.ItemName.GraphicsCardTier1);
        regItem(GRAPHICS_CARD_T2, Constants.ItemName.GraphicsCardTier2);
        regItem(GRAPHICS_CARD_T3, Constants.ItemName.GraphicsCardTier3);

        regItem(APU_T1, Constants.ItemName.APUTier1);
        regItem(APU_T2, Constants.ItemName.APUTier2);
        regItem(APU_CREATIVE, Constants.ItemName.APUCreative);

        regItem(COMPONENT_BUS_T1, Constants.ItemName.ComponentBusTier1);
        regItem(COMPONENT_BUS_T2, Constants.ItemName.ComponentBusTier2);
        regItem(COMPONENT_BUS_T3, Constants.ItemName.ComponentBusTier3);
        regItem(COMPONENT_BUS_CREATIVE, Constants.ItemName.ComponentBusCreative);

        regItem(NETWORK_CARD, Constants.ItemName.NetworkCard);
        regItem(WIRELESS_CARD_T1, Constants.ItemName.WirelessNetworkCardTier1);
        regItem(WIRELESS_CARD_T2, Constants.ItemName.WirelessNetworkCardTier2);

        regItem(REDSTONE_CARD_T1, Constants.ItemName.RedstoneCardTier1);
        regItem(REDSTONE_CARD_T2, Constants.ItemName.RedstoneCardTier2);

        regItem(DATA_CARD_T1, Constants.ItemName.DataCardTier1);
        regItem(DATA_CARD_T2, Constants.ItemName.DataCardTier2);
        regItem(DATA_CARD_T3, Constants.ItemName.DataCardTier3);

        regItem(ANALYZER, Constants.ItemName.Analyzer);
        regItem(DEBUG_CARD, Constants.ItemName.DebugCard);
        regItem(DEBUGGER, Constants.ItemName.Debugger);
        regItem(INTERNET_CARD, Constants.ItemName.InternetCard);
        regItem(LINKED_CARD, Constants.ItemName.LinkedCard);
        regItem(MANUAL, Constants.ItemName.Manual);
        regItem(NANOMACHINES, Constants.ItemName.Nanomachines);
        regItem(TEXTURE_PICKER, Constants.ItemName.TexturePicker);
        regItem(TPS_CARD, Constants.ItemName.TpsCard);
        regItem(WRENCH, Constants.ItemName.Wrench);

        regItem(FLOPPY, Constants.ItemName.Floppy);
        regItem(TERMINAL, Constants.ItemName.Terminal);
        regItem(TERMINAL_SERVER, Constants.ItemName.TerminalServer);
        regItem(DISK_DRIVE_MOUNTABLE, Constants.ItemName.DiskDriveMountable);

        regItem(INK_CARTRIDGE_EMPTY, Constants.ItemName.InkCartridgeEmpty);
        regItem(INK_CARTRIDGE, Constants.ItemName.InkCartridge);

        regItem(SERVER_T1, Constants.ItemName.ServerTier1);
        regItem(SERVER_T2, Constants.ItemName.ServerTier2);
        regItem(SERVER_T3, Constants.ItemName.ServerTier3);
        regItem(SERVER_CREATIVE, Constants.ItemName.ServerCreative);

        regItem(TABLET, Constants.ItemName.Tablet);
        regItem(TABLET_CASE_T1, Constants.ItemName.TabletCaseTier1);
        regItem(TABLET_CASE_T2, Constants.ItemName.TabletCaseTier2);
        regItem(TABLET_CASE_CREATIVE, Constants.ItemName.TabletCaseCreative);

        regItem(MC_CASE_T1, Constants.ItemName.MicrocontrollerCaseTier1);
        regItem(MC_CASE_T2, Constants.ItemName.MicrocontrollerCaseTier2);
        regItem(MC_CASE_CREATIVE, Constants.ItemName.MicrocontrollerCaseCreative);
        regItem(DRONE_CASE_T1, Constants.ItemName.DroneCaseTier1);
        regItem(DRONE_CASE_T2, Constants.ItemName.DroneCaseTier2);
        regItem(DRONE_CASE_CREATIVE, Constants.ItemName.DroneCaseCreative);
        regItem(DRONE, Constants.ItemName.Drone);

        regItem(PRESENT, Constants.ItemName.Present);

        regItem(UPGRADE_ANGEL, Constants.ItemName.AngelUpgrade);
        regItem(UPGRADE_CRAFTING, Constants.ItemName.CraftingUpgrade);
        regItem(UPGRADE_GENERATOR, Constants.ItemName.GeneratorUpgrade);
        regItem(UPGRADE_SOLAR, Constants.ItemName.SolarGeneratorUpgrade);
        regItem(UPGRADE_SIGN, Constants.ItemName.SignUpgrade);
        regItem(UPGRADE_NAV, Constants.ItemName.NavigationUpgrade);
        regItem(BATTERY_UPGRADE_T1, Constants.ItemName.BatteryUpgradeTier1);
        regItem(BATTERY_UPGRADE_T2, Constants.ItemName.BatteryUpgradeTier2);
        regItem(BATTERY_UPGRADE_T3, Constants.ItemName.BatteryUpgradeTier3);
        regItem(HOVER_UPGRADE_T1, Constants.ItemName.HoverUpgradeTier1);
        regItem(HOVER_UPGRADE_T2, Constants.ItemName.HoverUpgradeTier2);
        regItem(UPGRADE_CONTAINER_T1, Constants.ItemName.UpgradeContainerTier1);
        regItem(UPGRADE_CONTAINER_T2, Constants.ItemName.UpgradeContainerTier2);
        regItem(UPGRADE_CONTAINER_T3, Constants.ItemName.UpgradeContainerTier3);
        regItem(CARD_CONTAINER_T1, Constants.ItemName.CardContainerTier1);
        regItem(CARD_CONTAINER_T2, Constants.ItemName.CardContainerTier2);
        regItem(CARD_CONTAINER_T3, Constants.ItemName.CardContainerTier3);
        regItem(DATABASE_UPGRADE_T1, Constants.ItemName.DatabaseUpgradeTier1);
        regItem(DATABASE_UPGRADE_T2, Constants.ItemName.DatabaseUpgradeTier2);
        regItem(DATABASE_UPGRADE_T3, Constants.ItemName.DatabaseUpgradeTier3);
        regItem(UPGRADE_CHUNKLOADER, Constants.ItemName.ChunkloaderUpgrade);
        regItem(UPGRADE_EXPERIENCE, Constants.ItemName.ExperienceUpgrade);
        regItem(UPGRADE_INVENTORY, Constants.ItemName.InventoryUpgrade);
        regItem(UPGRADE_INVENTORY_CTRL, Constants.ItemName.InventoryControllerUpgrade);
        regItem(UPGRADE_LEASH, Constants.ItemName.LeashUpgrade);
        regItem(UPGRADE_MF, Constants.ItemName.MFU);
        regItem(UPGRADE_PISTON, Constants.ItemName.PistonUpgrade);
        regItem(UPGRADE_STICKY_PISTON, Constants.ItemName.StickyPistonUpgrade);
        regItem(UPGRADE_RITEG, Constants.ItemName.RITEGUpgrade);
        regItem(UPGRADE_TANK, Constants.ItemName.TankUpgrade);
        regItem(UPGRADE_TANK_CTRL, Constants.ItemName.TankControllerUpgrade);
        regItem(UPGRADE_TRACTOR, Constants.ItemName.TractorBeamUpgrade);
        regItem(UPGRADE_TRADING, Constants.ItemName.TradingUpgrade);

        regItem(HOVER_BOOTS, Constants.ItemName.HoverBoots);
        regItem(EEPROM_ITEM, Constants.ItemName.EEPROM);

        ItemStack luaBios = null;
        try (var stream = OpenComputers.class.getResourceAsStream(Settings.scriptPath + "bios.lua")) {
            if (stream != null) {
                var code = new byte[4 * 1024];
                var count = stream.read(code);
                luaBios = INSTANCE.registerEEPROM("EEPROM (Lua BIOS)", java.util.Arrays.copyOf(code, count), null, false);
            }
        } catch (Exception ex) {
            OpenComputers.log().error("Failed to load BIOS", ex);
        }
        if (luaBios != null) registerStack(luaBios, Constants.ItemName.LuaBios);

        for (var entry : aliases.entrySet()) {
            var alias = entry.getKey();
            var target = entry.getValue();
            if (!descriptors.containsKey(alias)) {
                var targetInfo = descriptors.get(target);
                if (targetInfo != null) descriptors.put(alias, targetInfo);
            }
        }
    }

    private static void regItem(DeferredHolder<Item, ?> holder, String itemName) {
        descriptors.put(itemName, new ItemInfo() {
            @Override
            public String name() {
                return itemName;
            }

            @Override
            public Block block() {
                return null;
            }

            @Override
            public Item item() {
                return holder.get();
            }

            @Override
            public ItemStack createItemStack(int size) {
                return new ItemStack(holder.get(), size);
            }
        });
        names.put(holder.get(), itemName);
    }

    @Override
    public ItemStack registerFloppy(String name, int color, Callable<FileSystem> factory, boolean doRecipeCycling) {
        ItemStack stack = Loot.registerLootDisk(name, color, factory, doRecipeCycling);
        registeredItems.add(stack);
        return stack.copy();
    }

    @Override
    public ItemStack registerFloppy(String name, int color, Callable<FileSystem> factory, boolean doRecipeCycling, String modId) {
        ItemStack stack = Loot.registerLootDisk(name, color, factory, doRecipeCycling, modId);
        registeredItems.add(stack);
        return stack.copy();
    }

    @Override
    public ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly) {
        CompoundTag nbt = new CompoundTag();
        if (name != null) nbt.putString(Settings.namespace + "label", name.substring(0, Math.min(name.length(), 24)));
        if (code != null) {
            byte[] trimmedCode = new byte[Math.min(code.length, Settings.get().eepromSize)];
            System.arraycopy(code, 0, trimmedCode, 0, trimmedCode.length);
            nbt.putByteArray(Settings.namespace + "eeprom", trimmedCode);
        }
        if (data != null) {
            byte[] trimmedData = new byte[Math.min(data.length, Settings.get().eepromDataSize)];
            System.arraycopy(data, 0, trimmedData, 0, trimmedData.length);
            nbt.putByteArray(Settings.namespace + "userdata", trimmedData);
        }
        nbt.putBoolean(Settings.namespace + "readonly", readonly);

        CompoundTag stackNbt = new CompoundTag();
        stackNbt.put(Settings.namespace + "data", nbt);

        ItemStack stack = get(Constants.ItemName.EEPROM).createItemStack(1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(stackNbt));

        registeredItems.add(stack);
        return stack.copy();
    }
}
