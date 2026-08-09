package li.cil.oc.fabric.common.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.SimpleBlock;
import li.cil.oc.core.impl.common.item.ALU;
import li.cil.oc.core.impl.common.item.APU;
import li.cil.oc.core.impl.common.item.Acid;
import li.cil.oc.core.impl.common.item.Analyzer;
import li.cil.oc.core.impl.common.item.ArrowKeys;
import li.cil.oc.core.impl.common.item.ButtonGroup;
import li.cil.oc.core.impl.common.item.CPU;
import li.cil.oc.core.impl.common.item.CardBase;
import li.cil.oc.core.impl.common.item.Chamelium;
import li.cil.oc.core.impl.common.item.CircuitBoard;
import li.cil.oc.core.impl.common.item.ComponentBus;
import li.cil.oc.core.impl.common.item.ControlUnit;
import li.cil.oc.core.impl.common.item.CuttingWire;
import li.cil.oc.core.impl.common.item.DataCard;
import li.cil.oc.core.impl.common.item.DebugCard;
import li.cil.oc.core.impl.common.item.Debugger;
import li.cil.oc.core.impl.common.item.DiamondChip;
import li.cil.oc.core.impl.common.item.Disk;
import li.cil.oc.core.impl.common.item.DiskDriveMountable;
import li.cil.oc.core.impl.common.item.Drone;
import li.cil.oc.core.impl.common.item.DroneCase;
import li.cil.oc.core.impl.common.item.EEPROM;
import li.cil.oc.core.impl.common.item.FloppyDisk;
import li.cil.oc.core.impl.common.item.GraphicsCard;
import li.cil.oc.core.impl.common.item.HardDiskDrive;
import li.cil.oc.core.impl.common.item.InkCartridge;
import li.cil.oc.core.impl.common.item.InkCartridgeEmpty;
import li.cil.oc.core.impl.common.item.InternetCard;
import li.cil.oc.core.impl.common.item.Interweb;
import li.cil.oc.core.impl.common.item.LinkedCard;
import li.cil.oc.core.impl.common.item.Manual;
import li.cil.oc.core.impl.common.item.Memory;
import li.cil.oc.core.impl.common.item.Microchip;
import li.cil.oc.core.impl.common.item.MicrocontrollerCase;
import li.cil.oc.core.impl.common.item.Nanomachines;
import li.cil.oc.core.impl.common.item.NetworkCard;
import li.cil.oc.core.impl.common.item.NumPad;
import li.cil.oc.core.impl.common.item.Present;
import li.cil.oc.core.impl.common.item.PrintedCircuitBoard;
import li.cil.oc.core.impl.common.item.RawCircuitBoard;
import li.cil.oc.core.impl.common.item.RedstoneCard;
import li.cil.oc.core.impl.common.item.Server;
import li.cil.oc.core.impl.common.item.Tablet;
import li.cil.oc.core.impl.common.item.TabletCase;
import li.cil.oc.core.impl.common.item.Terminal;
import li.cil.oc.core.impl.common.item.TerminalServer;
import li.cil.oc.core.impl.common.item.TexturePicker;
import li.cil.oc.core.impl.common.item.Transistor;
import li.cil.oc.core.impl.common.item.UpgradeAngel;
import li.cil.oc.core.impl.common.item.UpgradeBattery;
import li.cil.oc.core.impl.common.item.UpgradeChunkloader;
import li.cil.oc.core.impl.common.item.UpgradeContainerCard;
import li.cil.oc.core.impl.common.item.UpgradeContainerUpgrade;
import li.cil.oc.core.impl.common.item.UpgradeCrafting;
import li.cil.oc.core.impl.common.item.UpgradeDatabase;
import li.cil.oc.core.impl.common.item.UpgradeExperience;
import li.cil.oc.core.impl.common.item.UpgradeGenerator;
import li.cil.oc.core.impl.common.item.UpgradeHover;
import li.cil.oc.core.impl.common.item.UpgradeInventory;
import li.cil.oc.core.impl.common.item.UpgradeInventoryController;
import li.cil.oc.core.impl.common.item.UpgradeLeash;
import li.cil.oc.core.impl.common.item.UpgradeNavigation;
import li.cil.oc.core.impl.common.item.UpgradePiston;
import li.cil.oc.core.impl.common.item.UpgradeSign;
import li.cil.oc.core.impl.common.item.UpgradeSolarGenerator;
import li.cil.oc.core.impl.common.item.UpgradeStickyPiston;
import li.cil.oc.core.impl.common.item.UpgradeTank;
import li.cil.oc.core.impl.common.item.UpgradeTankController;
import li.cil.oc.core.impl.common.item.UpgradeTractorBeam;
import li.cil.oc.core.impl.common.item.UpgradeTrading;
import li.cil.oc.core.impl.common.item.WirelessNetworkCard;
import li.cil.oc.core.impl.common.item.Wrench;
import li.cil.oc.core.impl.common.item.data.DroneData;
import li.cil.oc.core.impl.common.item.data.HoverBootsData;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.common.item.HoverBoots;
import li.cil.oc.fabric.common.item.UpgradeMF;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public final class Items implements ItemAPI {
    private static final Item.Properties P = new Item.Properties();
    public static final Map<String, ItemInfo> descriptors = new HashMap<>();
    public static final Map<Object, String> names = new HashMap<>();
    public static final Map<String, String> aliases = new HashMap<>();
    public static final List<ItemStack> registeredItems = new ArrayList<>();

    static {
        aliases.put("dataCard", Constants.ItemName.DataCardTier1);
        aliases.put("wlanCard", Constants.ItemName.WirelessNetworkCardTier2);
    }

    private Items() {
    }

    private static <T extends Item> T reg(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name),
                item);
        return item;
    }

    private static Item.Properties r(int tier) {
        return new Item.Properties().rarity(li.cil.oc.core.impl.util.Rarity.byTier(tier));
    }

    private static Item.Properties r1(int tier) {
        return new Item.Properties().stacksTo(1).rarity(li.cil.oc.core.impl.util.Rarity.byTier(tier));
    }

    private static Item.Properties r2(int tier) {
        return new Item.Properties().rarity(li.cil.oc.core.impl.util.Rarity.byTier(tier / 2));
    }

    private static Item.Properties rr(Rarity rarity) {
        return new Item.Properties().rarity(rarity);
    }

    public static final ALU ALU = reg("alu", new ALU(P));
    public static final Acid ACID = reg("acid", new Acid(P));
    public static final ArrowKeys ARROW_KEYS = reg("arrowkeys", new ArrowKeys(P));
    public static final ButtonGroup BUTTON_GROUP = reg("buttongroup", new ButtonGroup(P));
    public static final CardBase CARD_BASE = reg("card", new CardBase(P));
    public static final Chamelium CHAMELIUM = reg("chamelium", new Chamelium(P));
    public static final CircuitBoard CIRCUIT_BOARD = reg("circuitboard", new CircuitBoard(P));
    public static final ControlUnit CONTROL_UNIT = reg("cu", new ControlUnit(P));
    public static final CuttingWire CUTTING_WIRE = reg("cuttingwire", new CuttingWire(P));
    public static final DiamondChip DIAMOND_CHIP = reg("chipdiamond", new DiamondChip(P));
    public static final Disk DISK = reg("disk", new Disk(P));
    public static final Interweb INTERWEB = reg("interweb", new Interweb(P));
    public static final Microchip MICROCHIP_T1 = reg("chip1", new Microchip(r(Tier.One), Tier.One));
    public static final Microchip MICROCHIP_T2 = reg("chip2", new Microchip(r(Tier.Two), Tier.Two));
    public static final Microchip MICROCHIP_T3 = reg("chip3", new Microchip(r(Tier.Three), Tier.Three));
    public static final NumPad NUM_PAD = reg("numpad", new NumPad(P));
    public static final PrintedCircuitBoard PRINTED_CIRCUIT_BOARD = reg("printedcircuitboard", new PrintedCircuitBoard(P));
    public static final RawCircuitBoard RAW_CIRCUIT_BOARD = reg("rawcircuitboard", new RawCircuitBoard(P));
    public static final Transistor TRANSISTOR = reg("transistor", new Transistor(P));

    public static final Memory RAM_T1 = reg("ram1", new Memory(r2(Tier.One), Tier.One));
    public static final Memory RAM_T2 = reg("ram2", new Memory(r2(Tier.Two), Tier.Two));
    public static final Memory RAM_T3 = reg("ram3", new Memory(r2(Tier.Three), Tier.Three));
    public static final Memory RAM_T4 = reg("ram4", new Memory(r2(Tier.Four), Tier.Four));
    public static final Memory RAM_T5 = reg("ram5", new Memory(r2(Tier.Five), Tier.Five));
    public static final Memory RAM_T6 = reg("ram6", new Memory(r2(Tier.Six), Tier.Six));

    public static final HardDiskDrive HDD_T1 = reg("hdd1", new HardDiskDrive(r(Tier.One), Tier.One));
    public static final HardDiskDrive HDD_T2 = reg("hdd2", new HardDiskDrive(r(Tier.Two), Tier.Two));
    public static final HardDiskDrive HDD_T3 = reg("hdd3", new HardDiskDrive(r(Tier.Three), Tier.Three));

    public static final CPU CPU_T1 = reg("cpu1", new CPU(r(Tier.One), Tier.One));
    public static final CPU CPU_T2 = reg("cpu2", new CPU(r(Tier.Two), Tier.Two));
    public static final CPU CPU_T3 = reg("cpu3", new CPU(r(Tier.Three), Tier.Three));

    public static final GraphicsCard GRAPHICS_CARD_T1 = reg("graphicscard1", new GraphicsCard(r(Tier.One), Tier.One));
    public static final GraphicsCard GRAPHICS_CARD_T2 = reg("graphicscard2", new GraphicsCard(r(Tier.Two), Tier.Two));
    public static final GraphicsCard GRAPHICS_CARD_T3 = reg("graphicscard3", new GraphicsCard(r(Tier.Three), Tier.Three));

    public static final APU APU_T1 = reg("apu1", new APU(r(Tier.One), Tier.One));
    public static final APU APU_T2 = reg("apu2", new APU(r(Tier.Two), Tier.Two));
    public static final APU APU_CREATIVE = reg("apucreative", new APU(rr(Rarity.EPIC), Tier.Three));

    public static final ComponentBus COMPONENT_BUS_T1 = reg("componentbus1", new ComponentBus(r(Tier.One), Tier.One));
    public static final ComponentBus COMPONENT_BUS_T2 = reg("componentbus2", new ComponentBus(r(Tier.Two), Tier.Two));
    public static final ComponentBus COMPONENT_BUS_T3 = reg("componentbus3", new ComponentBus(r(Tier.Three), Tier.Three));
    public static final ComponentBus COMPONENT_BUS_CREATIVE = reg("componentbuscreative", new ComponentBus(r(Tier.Four), Tier.Four));

    public static final NetworkCard NETWORK_CARD = reg("lancard", new NetworkCard(P));
    public static final WirelessNetworkCard WIRELESS_CARD_T1 = reg("wlancard1", new WirelessNetworkCard(r(Tier.One), Tier.One));
    public static final WirelessNetworkCard WIRELESS_CARD_T2 = reg("wlancard2", new WirelessNetworkCard(r(Tier.Two), Tier.Two));

    public static final RedstoneCard REDSTONE_CARD_T1 = reg("redstonecard1", new RedstoneCard(r(Tier.One), Tier.One));
    public static final RedstoneCard REDSTONE_CARD_T2 = reg("redstonecard2", new RedstoneCard(r(Tier.Two), Tier.Two));

    public static final DataCard DATA_CARD_T1 = reg("datacard1", new DataCard(r(Tier.One), Tier.One));
    public static final DataCard DATA_CARD_T2 = reg("datacard2", new DataCard(r(Tier.Two), Tier.Two));
    public static final DataCard DATA_CARD_T3 = reg("datacard3", new DataCard(r(Tier.Three), Tier.Three));

    public static final Analyzer ANALYZER = reg("analyzer", new Analyzer(P));
    public static final DebugCard DEBUG_CARD = reg("debugcard", new DebugCard(P));
    public static final Debugger DEBUGGER = reg("debugger", new Debugger(P));
    public static final InternetCard INTERNET_CARD = reg("internetcard", new InternetCard(rr(Rarity.UNCOMMON)));
    public static final LinkedCard LINKED_CARD = reg("linkedcard", new LinkedCard(rr(Rarity.RARE)));
    public static final Manual MANUAL = reg("manual", new Manual(P));
    public static final Nanomachines NANOMACHINES = reg("nanomachines", new Nanomachines(rr(Rarity.UNCOMMON)));
    public static final TexturePicker TEXTURE_PICKER = reg("texturepicker", new TexturePicker(P));
    public static final Wrench WRENCH = reg("wrench", new Wrench(P));

    public static final FloppyDisk FLOPPY = reg("floppy", new FloppyDisk(P));
    public static final Terminal TERMINAL = reg("terminal", new Terminal(P));
    public static final TerminalServer TERMINAL_SERVER = reg("terminalserver", new TerminalServer(P));
    public static final DiskDriveMountable DISK_DRIVE_MOUNTABLE = reg("diskdrivemountable", new DiskDriveMountable(new Item.Properties().stacksTo(1)));

    public static final InkCartridgeEmpty INK_CARTRIDGE_EMPTY = reg("inkcartridgeempty", new InkCartridgeEmpty(new Item.Properties().stacksTo(1)));
    public static final InkCartridge INK_CARTRIDGE = reg("inkcartridge", new InkCartridge(new Item.Properties().craftRemainder(INK_CARTRIDGE_EMPTY)));

    public static final Server SERVER_T1 = reg("server1", new Server(r1(Tier.One), Tier.One));
    public static final Server SERVER_T2 = reg("server2", new Server(r1(Tier.Two), Tier.Two));
    public static final Server SERVER_T3 = reg("server3", new Server(r1(Tier.Three), Tier.Three));
    public static final Server SERVER_CREATIVE = reg("servercreative", new Server(r1(Tier.Four), Tier.Four));

    public static final Tablet TABLET = reg("tablet", new Tablet(new Item.Properties().stacksTo(1)));
    public static final TabletCase TABLET_CASE_T1 = reg("tabletcase1", new TabletCase(r(Tier.One), Tier.One));
    public static final TabletCase TABLET_CASE_T2 = reg("tabletcase2", new TabletCase(r(Tier.Two), Tier.Two));
    public static final TabletCase TABLET_CASE_CREATIVE = reg("tabletcasecreative", new TabletCase(r(Tier.Four), Tier.Four));

    public static final MicrocontrollerCase MC_CASE_T1 = reg("microcontrollercase1", new MicrocontrollerCase(r(Tier.One), Tier.One));
    public static final MicrocontrollerCase MC_CASE_T2 = reg("microcontrollercase2", new MicrocontrollerCase(r(Tier.Two), Tier.Two));
    public static final MicrocontrollerCase MC_CASE_CREATIVE = reg("microcontrollercasecreative", new MicrocontrollerCase(r(Tier.Four), Tier.Four));
    public static final DroneCase DRONE_CASE_T1 = reg("dronecase1", new DroneCase(r(Tier.One), Tier.One));
    public static final DroneCase DRONE_CASE_T2 = reg("dronecase2", new DroneCase(r(Tier.Two), Tier.Two));
    public static final DroneCase DRONE_CASE_CREATIVE = reg("dronecasecreative", new DroneCase(r(Tier.Four), Tier.Four));
    public static final Drone DRONE = reg("drone", new Drone(P));

    public static final Present PRESENT = reg("present", new Present(P));

    public static final UpgradeAngel UPGRADE_ANGEL = reg("angelupgrade", new UpgradeAngel(rr(Rarity.UNCOMMON)));
    public static final UpgradeCrafting UPGRADE_CRAFTING = reg("craftingupgrade", new UpgradeCrafting(rr(Rarity.UNCOMMON)));
    public static final UpgradeGenerator UPGRADE_GENERATOR = reg("generatorupgrade", new UpgradeGenerator(rr(Rarity.UNCOMMON)));
    public static final UpgradeSolarGenerator UPGRADE_SOLAR = reg("solargeneratorupgrade", new UpgradeSolarGenerator(rr(Rarity.UNCOMMON)));
    public static final UpgradeSign UPGRADE_SIGN = reg("signupgrade", new UpgradeSign(P));
    public static final UpgradeNavigation UPGRADE_NAV = reg("navigationupgrade", new UpgradeNavigation(rr(Rarity.UNCOMMON)));

    public static final UpgradeBattery BATTERY_UPGRADE_T1 = reg("batteryupgrade1", new UpgradeBattery(r(Tier.One), Tier.One));
    public static final UpgradeBattery BATTERY_UPGRADE_T2 = reg("batteryupgrade2", new UpgradeBattery(r(Tier.Two), Tier.Two));
    public static final UpgradeBattery BATTERY_UPGRADE_T3 = reg("batteryupgrade3", new UpgradeBattery(r(Tier.Three), Tier.Three));

    public static final UpgradeHover HOVER_UPGRADE_T1 = reg("hoverupgrade1", new UpgradeHover(r(Tier.One), Tier.One));
    public static final UpgradeHover HOVER_UPGRADE_T2 = reg("hoverupgrade2", new UpgradeHover(r(Tier.Two), Tier.Two));

    public static final UpgradeContainerUpgrade UPGRADE_CONTAINER_T1 = reg("upgradecontainer1", new UpgradeContainerUpgrade(r(Tier.One), Tier.One));
    public static final UpgradeContainerUpgrade UPGRADE_CONTAINER_T2 = reg("upgradecontainer2", new UpgradeContainerUpgrade(r(Tier.Two), Tier.Two));
    public static final UpgradeContainerUpgrade UPGRADE_CONTAINER_T3 = reg("upgradecontainer3", new UpgradeContainerUpgrade(r(Tier.Three), Tier.Three));
    public static final UpgradeContainerCard CARD_CONTAINER_T1 = reg("cardcontainer1", new UpgradeContainerCard(r(Tier.One), Tier.One));
    public static final UpgradeContainerCard CARD_CONTAINER_T2 = reg("cardcontainer2", new UpgradeContainerCard(r(Tier.Two), Tier.Two));
    public static final UpgradeContainerCard CARD_CONTAINER_T3 = reg("cardcontainer3", new UpgradeContainerCard(r(Tier.Three), Tier.Three));

    public static final UpgradeDatabase DATABASE_UPGRADE_T1 = reg("databaseupgrade1", new UpgradeDatabase(r(Tier.One), Tier.One));
    public static final UpgradeDatabase DATABASE_UPGRADE_T2 = reg("databaseupgrade2", new UpgradeDatabase(r(Tier.Two), Tier.Two));
    public static final UpgradeDatabase DATABASE_UPGRADE_T3 = reg("databaseupgrade3", new UpgradeDatabase(r(Tier.Three), Tier.Three));

    public static final UpgradeChunkloader UPGRADE_CHUNKLOADER = reg("chunkloaderupgrade", new UpgradeChunkloader(rr(Rarity.RARE)));
    public static final UpgradeExperience UPGRADE_EXPERIENCE = reg("experienceupgrade", new UpgradeExperience(rr(Rarity.RARE)));
    public static final UpgradeInventory UPGRADE_INVENTORY = reg("inventoryupgrade", new UpgradeInventory(P));
    public static final UpgradeInventoryController UPGRADE_INVENTORY_CTRL = reg("inventorycontrollerupgrade", new UpgradeInventoryController(rr(Rarity.UNCOMMON)));
    public static final UpgradeLeash UPGRADE_LEASH = reg("leashupgrade", new UpgradeLeash(P));
    public static final UpgradeMF UPGRADE_MF = reg("mfu", new UpgradeMF(rr(Rarity.RARE)));
    public static final UpgradePiston UPGRADE_PISTON = reg("pistonupgrade", new UpgradePiston(P));
    public static final UpgradePiston UPGRADE_STICKY_PISTON = reg("stickypistonupgrade", new UpgradeStickyPiston(P));
    public static final UpgradeTank UPGRADE_TANK = reg("tankupgrade", new UpgradeTank(P));
    public static final UpgradeTankController UPGRADE_TANK_CTRL = reg("tankcontrollerupgrade", new UpgradeTankController(rr(Rarity.UNCOMMON)));
    public static final UpgradeTractorBeam UPGRADE_TRACTOR = reg("tractorbeamupgrade", new UpgradeTractorBeam(rr(Rarity.RARE)));
    public static final UpgradeTrading UPGRADE_TRADING = reg("tradingupgrade", new UpgradeTrading(rr(Rarity.UNCOMMON)));

    public static final EEPROM EEPROM_ITEM = reg("eeprom", new EEPROM());
    public static final HoverBoots HOVER_BOOTS = reg("hoverboots", new HoverBoots(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final Items INSTANCE = new Items();

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
        regItem(UPGRADE_TANK, Constants.ItemName.TankUpgrade);
        regItem(UPGRADE_TANK_CTRL, Constants.ItemName.TankControllerUpgrade);
        regItem(UPGRADE_TRACTOR, Constants.ItemName.TractorBeamUpgrade);
        regItem(UPGRADE_TRADING, Constants.ItemName.TradingUpgrade);

        regItem(HOVER_BOOTS, Constants.ItemName.HoverBoots);
        regItem(EEPROM_ITEM, Constants.ItemName.EEPROM);

        ItemStack luaBios = null;
        try (var stream = li.cil.oc.fabric.OpenComputers.class.getResourceAsStream(OCSettings.scriptPath + "bios.lua")) {
            if (stream != null) {
                var code = new byte[4 * 1024];
                var count = stream.read(code);
                luaBios = INSTANCE.registerEEPROM("EEPROM (Lua BIOS)", java.util.Arrays.copyOf(code, count), null, false);
            }
        } catch (Exception ex) {
            li.cil.oc.fabric.OpenComputers.log().error("Failed to load BIOS", ex);
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

    private static void regItem(Item item, String itemName) {
        descriptors.put(itemName, new ItemInfo() {
            @Override
            public String name() {
                return itemName;
            }

            @SuppressWarnings("SameReturnValue")
            @Override
            public Block block() {
                return null;
            }

            @Override
            public Item item() {
                return item;
            }

            @Override
            public ItemStack createItemStack(int size) {
                return new ItemStack(item, size);
            }
        });
        names.put(item, itemName);
    }

    public static void registerBlock(Block instance, String id) {
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
                return instance.asItem();
            }

            @Override
            public ItemStack createItemStack(int size) {
                if (instance instanceof SimpleBlock sb) return sb.createItemStack(size);
                return new ItemStack(instance, size);
            }
        });
        names.put(instance, id);
    }

    @Override
    public ItemInfo get(String name) {
        return descriptors.get(name);
    }

    @Override
    public ItemInfo get(ItemStack stack) {
        if (stack == null) return null;
        Object key;
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            key = bi.getBlock();
        } else {
            key = stack.getItem();
        }
        String name = names.get(key);
        return name != null ? get(name) : null;
    }

    @Override
    public ItemStack registerFloppy(String name, DyeColor color, Callable<FileSystem> factory, boolean doRecipeCycling) {
        ItemStack stack = li.cil.oc.fabric.common.Loot.registerLootDisk(name, color.getId(), factory, doRecipeCycling);
        registeredItems.add(stack);
        return stack.copy();
    }

    @Override
    public ItemStack registerFloppy(String name, DyeColor color, Callable<FileSystem> factory, boolean doRecipeCycling, String modId) {
        ItemStack stack = li.cil.oc.fabric.common.Loot.registerLootDisk(name, color.getId(), factory, doRecipeCycling, modId);
        registeredItems.add(stack);
        return stack.copy();
    }

    @Override
    public ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly) {
        CompoundTag nbt = new CompoundTag();
        if (name != null) nbt.putString(OCSettings.namespace + "label", name.substring(0, Math.min(name.length(), 24)));
        if (code != null) {
            byte[] trimmedCode = new byte[Math.min(code.length, OCSettings.get().eepromSize)];
            System.arraycopy(code, 0, trimmedCode, 0, trimmedCode.length);
            nbt.putByteArray(OCSettings.namespace + "eeprom", trimmedCode);
        }
        if (data != null) {
            byte[] trimmedData = new byte[Math.min(data.length, OCSettings.get().eepromDataSize)];
            System.arraycopy(data, 0, trimmedData, 0, trimmedData.length);
            nbt.putByteArray(OCSettings.namespace + "userdata", trimmedData);
        }
        nbt.putBoolean(OCSettings.namespace + "readonly", readonly);

        CompoundTag stackNbt = new CompoundTag();
        stackNbt.put(OCSettings.namespace + "data", nbt);

        ItemStack stack = get(Constants.ItemName.EEPROM).createItemStack(1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(stackNbt));

        if (!descriptors.containsKey(Constants.ItemName.LuaBios)) {
            registerStack(stack, Constants.ItemName.LuaBios);
        }

        return stack.copy();
    }

    public static void registerStack(ItemStack stack, String id) {
        ItemStack immutableStack = stack.copy();
        descriptors.put(id, new ItemInfo() {
            @Override
            public String name() {
                return id;
            }

            @SuppressWarnings("SameReturnValue")
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

    private static ItemStack safeGetStack(String name) {
        ItemInfo info = li.cil.oc.api.API.items.get(name);
        return info != null ? info.createItemStack(1) : null;
    }

    public static ItemStack createConfiguredDrone() {
        DroneData data = new DroneData();
        data.name = "Crecopter";
        data.tier = Tier.Four;
        data.storedEnergy = (int) OCSettings.get().bufferDrone;
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
        data.storedEnergy = (int) OCSettings.get().bufferMicrocontroller;
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
        data.robotEnergy = (int) OCSettings.get().bufferRobot;
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
        data.energy = OCSettings.get().bufferTablet;
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
        data.charge = OCSettings.get().bufferHoverBoots;
        return data.createItemStack();
    }
}
