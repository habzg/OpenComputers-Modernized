package li.cil.oc.neoforge.common.init;

import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.block.Adapter;
import li.cil.oc.neoforge.common.block.Assembler;
import li.cil.oc.neoforge.common.block.Cable;
import li.cil.oc.neoforge.common.block.Capacitor;
import li.cil.oc.neoforge.common.block.CarpetedCapacitor;
import li.cil.oc.neoforge.common.block.Case;
import li.cil.oc.neoforge.common.block.ChameliumBlock;
import li.cil.oc.neoforge.common.block.Charger;
import li.cil.oc.neoforge.common.block.Disassembler;
import li.cil.oc.neoforge.common.block.DiskDrive;
import li.cil.oc.neoforge.common.block.FakeEndstone;
import li.cil.oc.neoforge.common.block.Geolyzer;
import li.cil.oc.neoforge.common.block.Hologram;
import li.cil.oc.neoforge.common.block.Keyboard;
import li.cil.oc.neoforge.common.block.Microcontroller;
import li.cil.oc.neoforge.common.block.MotionSensor;
import li.cil.oc.neoforge.common.block.NetSplitter;
import li.cil.oc.neoforge.common.block.PowerConverter;
import li.cil.oc.neoforge.common.block.PowerDistributor;
import li.cil.oc.neoforge.common.block.Print;
import li.cil.oc.neoforge.common.block.Printer;
import li.cil.oc.neoforge.common.block.Rack;
import li.cil.oc.neoforge.common.block.Raid;
import li.cil.oc.neoforge.common.block.Redstone;
import li.cil.oc.neoforge.common.block.Relay;
import li.cil.oc.neoforge.common.block.RobotAfterimage;
import li.cil.oc.neoforge.common.block.RobotProxy;
import li.cil.oc.neoforge.common.block.Screen;
import li.cil.oc.neoforge.common.block.Transposer;
import li.cil.oc.neoforge.common.block.Waypoint;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;

public final class Blocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, OpenComputers.ID);

    private Blocks() {
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final DeferredHolder<Block, Adapter> ADAPTER =
            BLOCKS.register(key(Constants.BlockName.Adapter), Adapter::new);
    public static final DeferredHolder<Block, Assembler> ASSEMBLER =
            BLOCKS.register(key(Constants.BlockName.Assembler), Assembler::new);
    public static final DeferredHolder<Block, Cable> CABLE =
            BLOCKS.register(key(Constants.BlockName.Cable), Cable::new);
    public static final DeferredHolder<Block, Capacitor> CAPACITOR =
            BLOCKS.register(key(Constants.BlockName.Capacitor), Capacitor::new);
    public static final DeferredHolder<Block, Case> CASE_TIER_1 =
            BLOCKS.register(key(Constants.BlockName.CaseTier1), () -> new Case(Tier.One));
    public static final DeferredHolder<Block, Case> CASE_TIER_3 =
            BLOCKS.register(key(Constants.BlockName.CaseTier3), () -> new Case(Tier.Three));
    public static final DeferredHolder<Block, Case> CASE_TIER_2 =
            BLOCKS.register(key(Constants.BlockName.CaseTier2), () -> new Case(Tier.Two));
    public static final DeferredHolder<Block, Charger> CHARGER =
            BLOCKS.register(key(Constants.BlockName.Charger), Charger::new);
    public static final DeferredHolder<Block, Disassembler> DISASSEMBLER =
            BLOCKS.register(key(Constants.BlockName.Disassembler), Disassembler::new);
    public static final DeferredHolder<Block, DiskDrive> DISK_DRIVE =
            BLOCKS.register(key(Constants.BlockName.DiskDrive), DiskDrive::new);
    public static final DeferredHolder<Block, Geolyzer> GEOLYZER =
            BLOCKS.register(key(Constants.BlockName.Geolyzer), Geolyzer::new);
    public static final DeferredHolder<Block, Hologram> HOLOGRAM_TIER_1 =
            BLOCKS.register(key(Constants.BlockName.HologramTier1), () -> new Hologram(Tier.One));
    public static final DeferredHolder<Block, Hologram> HOLOGRAM_TIER_2 =
            BLOCKS.register(key(Constants.BlockName.HologramTier2), () -> new Hologram(Tier.Two));
    public static final DeferredHolder<Block, Keyboard> KEYBOARD =
            BLOCKS.register(key(Constants.BlockName.Keyboard), Keyboard::new);
    public static final DeferredHolder<Block, MotionSensor> MOTION_SENSOR =
            BLOCKS.register(key(Constants.BlockName.MotionSensor), MotionSensor::new);
    public static final DeferredHolder<Block, PowerConverter> POWER_CONVERTER =
            BLOCKS.register(key(Constants.BlockName.PowerConverter), PowerConverter::new);
    public static final DeferredHolder<Block, PowerDistributor> POWER_DISTRIBUTOR =
            BLOCKS.register(key(Constants.BlockName.PowerDistributor), PowerDistributor::new);
    public static final DeferredHolder<Block, Raid> RAID =
            BLOCKS.register(key(Constants.BlockName.Raid), Raid::new);
    public static final DeferredHolder<Block, Redstone> REDSTONE =
            BLOCKS.register(key(Constants.BlockName.Redstone), Redstone::new);
    public static final DeferredHolder<Block, Relay> RELAY =
            BLOCKS.register(key(Constants.BlockName.Relay), Relay::new);
    public static final DeferredHolder<Block, Screen> SCREEN_TIER_1 =
            BLOCKS.register(key(Constants.BlockName.ScreenTier1), () -> new Screen(Tier.One));
    public static final DeferredHolder<Block, Screen> SCREEN_TIER_3 =
            BLOCKS.register(key(Constants.BlockName.ScreenTier3), () -> new Screen(Tier.Three));
    public static final DeferredHolder<Block, Screen> SCREEN_TIER_2 =
            BLOCKS.register(key(Constants.BlockName.ScreenTier2), () -> new Screen(Tier.Two));
    public static final DeferredHolder<Block, Rack> RACK =
            BLOCKS.register(key(Constants.BlockName.Rack), Rack::new);
    public static final DeferredHolder<Block, Case> CASE_CREATIVE =
            BLOCKS.register(key(Constants.BlockName.CaseCreative), () -> new Case(Tier.Four));
    public static final DeferredHolder<Block, Microcontroller> MICROCONTROLLER =
            BLOCKS.register(key(Constants.BlockName.Microcontroller), Microcontroller::new);
    public static final DeferredHolder<Block, RobotAfterimage> ROBOT_AFTERIMAGE =
            BLOCKS.register(key(Constants.BlockName.RobotAfterimage), RobotAfterimage::new);
    public static final DeferredHolder<Block, RobotProxy> ROBOT =
            BLOCKS.register(key(Constants.BlockName.Robot), RobotProxy::new);
    public static final DeferredHolder<Block, Print> PRINT =
            BLOCKS.register(key(Constants.BlockName.Print), Print::new);
    public static final DeferredHolder<Block, Print> BEACON_BASE_PRINT =
            BLOCKS.register(key(Constants.BlockName.BeaconBasePrint), Print::new);
    public static final DeferredHolder<Block, Printer> PRINTER =
            BLOCKS.register(key(Constants.BlockName.Printer), Printer::new);
    public static final DeferredHolder<Block, ChameliumBlock> CHAMELIUM_BLOCK =
            BLOCKS.register(key(Constants.BlockName.ChameliumBlock), ChameliumBlock::new);
    public static final DeferredHolder<Block, Waypoint> WAYPOINT =
            BLOCKS.register(key(Constants.BlockName.Waypoint), Waypoint::new);
    public static final DeferredHolder<Block, FakeEndstone> ENDSTONE =
            BLOCKS.register(key(Constants.BlockName.Endstone), FakeEndstone::new);
    public static final DeferredHolder<Block, NetSplitter> NET_SPLITTER =
            BLOCKS.register(key(Constants.BlockName.NetSplitter), NetSplitter::new);
    public static final DeferredHolder<Block, Transposer> TRANSPOSER =
            BLOCKS.register(key(Constants.BlockName.Transposer), Transposer::new);
    public static final DeferredHolder<Block, CarpetedCapacitor> CARPETED_CAPACITOR =
            BLOCKS.register(key(Constants.BlockName.CarpetedCapacitor), CarpetedCapacitor::new);

    public static void init() {
        Items.registerBlock(ADAPTER.get(), Constants.BlockName.Adapter);
        Items.registerBlock(ASSEMBLER.get(), Constants.BlockName.Assembler);
        Items.registerBlock(CABLE.get(), Constants.BlockName.Cable);
        Items.registerBlock(CAPACITOR.get(), Constants.BlockName.Capacitor);
        Items.registerBlock(CASE_TIER_1.get(), Constants.BlockName.CaseTier1);
        Items.registerBlock(CASE_TIER_3.get(), Constants.BlockName.CaseTier3);
        Items.registerBlock(CASE_TIER_2.get(), Constants.BlockName.CaseTier2);
        Items.registerBlock(CHARGER.get(), Constants.BlockName.Charger);
        Items.registerBlock(DISASSEMBLER.get(), Constants.BlockName.Disassembler);
        Items.registerBlock(DISK_DRIVE.get(), Constants.BlockName.DiskDrive);
        Items.registerBlock(GEOLYZER.get(), Constants.BlockName.Geolyzer);
        Items.registerBlock(HOLOGRAM_TIER_1.get(), Constants.BlockName.HologramTier1);
        Items.registerBlock(HOLOGRAM_TIER_2.get(), Constants.BlockName.HologramTier2);
        Items.registerBlock(KEYBOARD.get(), Constants.BlockName.Keyboard);
        Items.registerBlock(MOTION_SENSOR.get(), Constants.BlockName.MotionSensor);
        Items.registerBlock(POWER_CONVERTER.get(), Constants.BlockName.PowerConverter);
        Items.registerBlock(POWER_DISTRIBUTOR.get(), Constants.BlockName.PowerDistributor);
        Items.registerBlock(RAID.get(), Constants.BlockName.Raid);
        Items.registerBlock(REDSTONE.get(), Constants.BlockName.Redstone);
        Items.registerBlock(RELAY.get(), Constants.BlockName.Relay);
        Items.registerBlock(SCREEN_TIER_1.get(), Constants.BlockName.ScreenTier1);
        Items.registerBlock(SCREEN_TIER_3.get(), Constants.BlockName.ScreenTier3);
        Items.registerBlock(SCREEN_TIER_2.get(), Constants.BlockName.ScreenTier2);
        Items.registerBlock(RACK.get(), Constants.BlockName.Rack);
        Items.registerBlock(CASE_CREATIVE.get(), Constants.BlockName.CaseCreative);
        Items.registerBlock(MICROCONTROLLER.get(), Constants.BlockName.Microcontroller);
        Items.registerBlock(ROBOT_AFTERIMAGE.get(), Constants.BlockName.RobotAfterimage);
        Items.registerBlock(ROBOT.get(), Constants.BlockName.Robot);
        Items.registerBlock(PRINT.get(), "print");
        Items.registerBlock(BEACON_BASE_PRINT.get(), Constants.BlockName.BeaconBasePrint);
        Items.registerBlock(PRINTER.get(), "printer");
        Items.registerBlock(CHAMELIUM_BLOCK.get(), "chameliumBlock");
        Items.registerBlock(WAYPOINT.get(), Constants.BlockName.Waypoint);
        Items.registerBlock(ENDSTONE.get(), Constants.BlockName.Endstone);
        Items.registerBlock(NET_SPLITTER.get(), Constants.BlockName.NetSplitter);
        Items.registerBlock(TRANSPOSER.get(), Constants.BlockName.Transposer);
        Items.registerBlock(CARPETED_CAPACITOR.get(), Constants.BlockName.CarpetedCapacitor);
    }
}
