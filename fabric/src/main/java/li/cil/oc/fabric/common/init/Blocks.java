package li.cil.oc.fabric.common.init;

import java.util.Locale;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.block.Adapter;
import li.cil.oc.core.impl.common.block.Assembler;
import li.cil.oc.core.impl.common.block.Capacitor;
import li.cil.oc.core.impl.common.block.CarpetedCapacitor;
import li.cil.oc.core.impl.common.block.ChameliumBlock;
import li.cil.oc.core.impl.common.block.Charger;
import li.cil.oc.core.impl.common.block.Disassembler;
import li.cil.oc.core.impl.common.block.DiskDrive;
import li.cil.oc.core.impl.common.block.FakeEndstone;
import li.cil.oc.core.impl.common.block.Geolyzer;
import li.cil.oc.core.impl.common.block.Hologram;
import li.cil.oc.core.impl.common.block.Keyboard;
import li.cil.oc.core.impl.common.block.Microcontroller;
import li.cil.oc.core.impl.common.block.MotionSensor;
import li.cil.oc.core.impl.common.block.PowerConverter;
import li.cil.oc.core.impl.common.block.PowerDistributor;
import li.cil.oc.core.impl.common.block.Printer;
import li.cil.oc.core.impl.common.block.Rack;
import li.cil.oc.core.impl.common.block.Raid;
import li.cil.oc.core.impl.common.block.Redstone;
import li.cil.oc.core.impl.common.block.Screen;
import li.cil.oc.core.impl.common.block.Waypoint;
import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.common.block.Cable;
import li.cil.oc.fabric.common.block.Case;
import li.cil.oc.fabric.common.block.NetSplitter;
import li.cil.oc.fabric.common.block.Print;
import li.cil.oc.fabric.common.block.Relay;
import li.cil.oc.fabric.common.block.RobotAfterimage;
import li.cil.oc.fabric.common.block.RobotProxy;
import li.cil.oc.fabric.common.block.Transposer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class Blocks {
    public static Block[] ALL_BLOCKS;

    private Blocks() {
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static <T extends Block> T register(String name, T block) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new li.cil.oc.core.impl.common.block.Item(block));
        return block;
    }

    public static final Adapter ADAPTER = register(key(Constants.BlockName.Adapter), new Adapter(null));
    public static final Assembler ASSEMBLER = register(key(Constants.BlockName.Assembler), new Assembler(null));
    public static final Capacitor CAPACITOR = register(key(Constants.BlockName.Capacitor), new Capacitor());
    public static final Charger CHARGER = register(key(Constants.BlockName.Charger), new Charger(null));
    public static final Disassembler DISASSEMBLER = register(key(Constants.BlockName.Disassembler), new Disassembler(null));
    public static final DiskDrive DISK_DRIVE = register(key(Constants.BlockName.DiskDrive), new DiskDrive());
    public static final Geolyzer GEOLYZER = register(key(Constants.BlockName.Geolyzer), new Geolyzer());
    public static final Hologram HOLOGRAM_TIER_1 = register(key(Constants.BlockName.HologramTier1), new Hologram(li.cil.oc.core.common.Tier.One, null));
    public static final Hologram HOLOGRAM_TIER_2 = register(key(Constants.BlockName.HologramTier2), new Hologram(li.cil.oc.core.common.Tier.Two, null));
    public static final Keyboard KEYBOARD = register(key(Constants.BlockName.Keyboard), new Keyboard());
    public static final MotionSensor MOTION_SENSOR = register(key(Constants.BlockName.MotionSensor), new MotionSensor(null));
    public static final PowerConverter POWER_CONVERTER = register(key(Constants.BlockName.PowerConverter), new PowerConverter(null));
    public static final PowerDistributor POWER_DISTRIBUTOR = register(key(Constants.BlockName.PowerDistributor), new PowerDistributor(null));
    public static final Raid RAID = register(key(Constants.BlockName.Raid), new Raid());
    public static final Redstone REDSTONE = register(key(Constants.BlockName.Redstone), new Redstone(null));
    public static final Screen SCREEN_TIER_1 = register(key(Constants.BlockName.ScreenTier1), new Screen(li.cil.oc.core.common.Tier.One, null));
    public static final Screen SCREEN_TIER_3 = register(key(Constants.BlockName.ScreenTier3), new Screen(li.cil.oc.core.common.Tier.Three, null));
    public static final Screen SCREEN_TIER_2 = register(key(Constants.BlockName.ScreenTier2), new Screen(li.cil.oc.core.common.Tier.Two, null));
    public static final Rack RACK = register(key(Constants.BlockName.Rack), new Rack(null));
    public static final Microcontroller MICROCONTROLLER = register(key(Constants.BlockName.Microcontroller), new Microcontroller(null));
    public static final Printer PRINTER = register(key(Constants.BlockName.Printer), new Printer(null));
    public static final ChameliumBlock CHAMELIUM_BLOCK = register(key(Constants.BlockName.ChameliumBlock), new ChameliumBlock());
    public static final Waypoint WAYPOINT = register(key(Constants.BlockName.Waypoint), new Waypoint(null));
    public static final FakeEndstone ENDSTONE = register(key(Constants.BlockName.Endstone), new FakeEndstone());
    public static final NetSplitter NET_SPLITTER = register(key(Constants.BlockName.NetSplitter), new NetSplitter());
    public static final CarpetedCapacitor CARPETED_CAPACITOR = register(key(Constants.BlockName.CarpetedCapacitor), new CarpetedCapacitor(null));
    public static final Cable CABLE = register(key(Constants.BlockName.Cable), new Cable());
    public static final Case CASE_TIER_1 = register(key(Constants.BlockName.CaseTier1), new Case(li.cil.oc.core.common.Tier.One));
    public static final Case CASE_TIER_2 = register(key(Constants.BlockName.CaseTier2), new Case(li.cil.oc.core.common.Tier.Two));
    public static final Case CASE_TIER_3 = register(key(Constants.BlockName.CaseTier3), new Case(li.cil.oc.core.common.Tier.Three));
    public static final Case CASE_CREATIVE = register(key(Constants.BlockName.CaseCreative), new Case(li.cil.oc.core.common.Tier.Four));
    public static final Print PRINT = register(key(Constants.BlockName.Print), new Print());
    public static final Print BEACON_BASE_PRINT = register(key(Constants.BlockName.BeaconBasePrint), new Print());
    public static final Relay RELAY = register(key(Constants.BlockName.Relay), new Relay());
    public static final RobotProxy ROBOT = register(key(Constants.BlockName.Robot), new RobotProxy());
    public static final RobotAfterimage ROBOT_AFTERIMAGE = register(key(Constants.BlockName.RobotAfterimage), new RobotAfterimage());
    public static final Transposer TRANSPOSER = register(key(Constants.BlockName.Transposer), new Transposer());

    public static void init() {
        ALL_BLOCKS = new Block[]{
                ADAPTER, ASSEMBLER, CABLE, CAPACITOR, CASE_TIER_1, CASE_TIER_2, CASE_TIER_3, CASE_CREATIVE,
                CARPETED_CAPACITOR, CHAMELIUM_BLOCK, CHARGER, DISASSEMBLER, DISK_DRIVE, ENDSTONE, GEOLYZER,
                HOLOGRAM_TIER_1, HOLOGRAM_TIER_2, KEYBOARD, MICROCONTROLLER, MOTION_SENSOR, NET_SPLITTER,
                POWER_CONVERTER, POWER_DISTRIBUTOR, PRINT, BEACON_BASE_PRINT, PRINTER, RACK, RAID, REDSTONE,
                RELAY, ROBOT, ROBOT_AFTERIMAGE, SCREEN_TIER_1, SCREEN_TIER_2, SCREEN_TIER_3, TRANSPOSER, WAYPOINT
        };
        Items.registerBlock(ADAPTER, Constants.BlockName.Adapter);
        Items.registerBlock(ASSEMBLER, Constants.BlockName.Assembler);
        Items.registerBlock(CABLE, Constants.BlockName.Cable);
        Items.registerBlock(CAPACITOR, Constants.BlockName.Capacitor);
        Items.registerBlock(CASE_TIER_1, Constants.BlockName.CaseTier1);
        Items.registerBlock(CASE_TIER_2, Constants.BlockName.CaseTier2);
        Items.registerBlock(CASE_TIER_3, Constants.BlockName.CaseTier3);
        Items.registerBlock(CASE_CREATIVE, Constants.BlockName.CaseCreative);
        Items.registerBlock(CARPETED_CAPACITOR, Constants.BlockName.CarpetedCapacitor);
        Items.registerBlock(CHAMELIUM_BLOCK, Constants.BlockName.ChameliumBlock);
        Items.registerBlock(CHARGER, Constants.BlockName.Charger);
        Items.registerBlock(DISASSEMBLER, Constants.BlockName.Disassembler);
        Items.registerBlock(DISK_DRIVE, Constants.BlockName.DiskDrive);
        Items.registerBlock(ENDSTONE, Constants.BlockName.Endstone);
        Items.registerBlock(GEOLYZER, Constants.BlockName.Geolyzer);
        Items.registerBlock(HOLOGRAM_TIER_1, Constants.BlockName.HologramTier1);
        Items.registerBlock(HOLOGRAM_TIER_2, Constants.BlockName.HologramTier2);
        Items.registerBlock(KEYBOARD, Constants.BlockName.Keyboard);
        Items.registerBlock(MICROCONTROLLER, Constants.BlockName.Microcontroller);
        Items.registerBlock(MOTION_SENSOR, Constants.BlockName.MotionSensor);
        Items.registerBlock(NET_SPLITTER, Constants.BlockName.NetSplitter);
        Items.registerBlock(POWER_CONVERTER, Constants.BlockName.PowerConverter);
        Items.registerBlock(POWER_DISTRIBUTOR, Constants.BlockName.PowerDistributor);
        Items.registerBlock(PRINT, "print");
        Items.registerBlock(BEACON_BASE_PRINT, Constants.BlockName.BeaconBasePrint);
        Items.registerBlock(PRINTER, "printer");
        Items.registerBlock(RACK, Constants.BlockName.Rack);
        Items.registerBlock(RAID, Constants.BlockName.Raid);
        Items.registerBlock(REDSTONE, Constants.BlockName.Redstone);
        Items.registerBlock(RELAY, Constants.BlockName.Relay);
        Items.registerBlock(ROBOT, Constants.BlockName.Robot);
        Items.registerBlock(ROBOT_AFTERIMAGE, Constants.BlockName.RobotAfterimage);
        Items.registerBlock(SCREEN_TIER_1, Constants.BlockName.ScreenTier1);
        Items.registerBlock(SCREEN_TIER_2, Constants.BlockName.ScreenTier2);
        Items.registerBlock(SCREEN_TIER_3, Constants.BlockName.ScreenTier3);
        Items.registerBlock(TRANSPOSER, Constants.BlockName.Transposer);
        Items.registerBlock(WAYPOINT, Constants.BlockName.Waypoint);
    }
}
