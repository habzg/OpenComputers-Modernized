package li.cil.oc.fabric.common.init;

import java.util.Locale;
import li.cil.oc.core.impl.common.blockentity.Adapter;
import li.cil.oc.core.impl.common.blockentity.Assembler;
import li.cil.oc.core.impl.common.blockentity.Cable;
import li.cil.oc.core.impl.common.blockentity.Capacitor;
import li.cil.oc.core.impl.common.blockentity.CarpetedCapacitor;
import li.cil.oc.core.impl.common.blockentity.Case;
import li.cil.oc.core.impl.common.blockentity.Charger;
import li.cil.oc.core.impl.common.blockentity.Disassembler;
import li.cil.oc.core.impl.common.blockentity.DiskDrive;
import li.cil.oc.core.impl.common.blockentity.Geolyzer;
import li.cil.oc.core.impl.common.blockentity.Hologram;
import li.cil.oc.core.impl.common.blockentity.Keyboard;
import li.cil.oc.core.impl.common.blockentity.Microcontroller;
import li.cil.oc.core.impl.common.blockentity.MotionSensor;
import li.cil.oc.core.impl.common.blockentity.NetSplitter;
import li.cil.oc.core.impl.common.blockentity.PowerConverter;
import li.cil.oc.core.impl.common.blockentity.PowerDistributor;
import li.cil.oc.core.impl.common.blockentity.Print;
import li.cil.oc.core.impl.common.blockentity.Printer;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.core.impl.common.blockentity.Raid;
import li.cil.oc.core.impl.common.blockentity.Redstone;
import li.cil.oc.core.impl.common.blockentity.Screen;
import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.common.blockentity.CaseTile;
import li.cil.oc.fabric.common.blockentity.NetSplitterTile;
import li.cil.oc.fabric.common.blockentity.PrintFabric;
import li.cil.oc.fabric.common.blockentity.RedstoneTile;
import li.cil.oc.fabric.common.blockentity.RobotProxy;
import li.cil.oc.fabric.common.blockentity.ScreenTile;
import li.cil.oc.fabric.common.blockentity.Transposer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockEntities {
    private BlockEntities() {
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name, BlockEntityType.Builder<T> builder) {
        BlockEntityType<T> type = builder.build(null);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name.toLowerCase(Locale.ROOT));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
        return type;
    }

    public static final BlockEntityType<Adapter> ADAPTER = register("Adapter",
            BlockEntityType.Builder.of(Adapter::new, Blocks.ADAPTER));
    public static final BlockEntityType<Assembler> ASSEMBLER = register("Assembler",
            BlockEntityType.Builder.of(Assembler::new, Blocks.ASSEMBLER));
    public static final BlockEntityType<Capacitor> CAPACITOR = register("Capacitor",
            BlockEntityType.Builder.of(Capacitor::new, Blocks.CAPACITOR));
    public static final BlockEntityType<Cable> CABLE = register("Cable",
            BlockEntityType.Builder.of(Cable::new, Blocks.CABLE));
    public static final BlockEntityType<Case> CASE = register("Case",
            BlockEntityType.Builder.of((pos, state) -> new CaseTile(pos, state, 0),
                    Blocks.CASE_TIER_1, Blocks.CASE_TIER_2, Blocks.CASE_TIER_3, Blocks.CASE_CREATIVE));
    public static final BlockEntityType<CarpetedCapacitor> CARPETED_CAPACITOR = register("CarpetedCapacitor",
            BlockEntityType.Builder.of(CarpetedCapacitor::new, Blocks.CARPETED_CAPACITOR));
    public static final BlockEntityType<Charger> CHARGER = register("Charger",
            BlockEntityType.Builder.of(Charger::new, Blocks.CHARGER));
    public static final BlockEntityType<Disassembler> DISASSEMBLER = register("Disassembler",
            BlockEntityType.Builder.of(Disassembler::new, Blocks.DISASSEMBLER));
    public static final BlockEntityType<DiskDrive> DISK_DRIVE = register("DiskDrive",
            BlockEntityType.Builder.of(DiskDrive::new, Blocks.DISK_DRIVE));
    public static final BlockEntityType<Geolyzer> GEOLYZER = register("Geolyzer",
            BlockEntityType.Builder.of(Geolyzer::new, Blocks.GEOLYZER));
    public static final BlockEntityType<Hologram> HOLOGRAM = register("Hologram",
            BlockEntityType.Builder.of(Hologram::new, Blocks.HOLOGRAM_TIER_1, Blocks.HOLOGRAM_TIER_2));
    public static final BlockEntityType<Keyboard> KEYBOARD = register("Keyboard",
            BlockEntityType.Builder.of(Keyboard::new, Blocks.KEYBOARD));
    public static final BlockEntityType<Microcontroller> MICROCONTROLLER = register("Microcontroller",
            BlockEntityType.Builder.of(Microcontroller::new, Blocks.MICROCONTROLLER));
    public static final BlockEntityType<MotionSensor> MOTION_SENSOR = register("MotionSensor",
            BlockEntityType.Builder.of(MotionSensor::new, Blocks.MOTION_SENSOR));
    public static final BlockEntityType<NetSplitter> NET_SPLITTER = register("NetSplitter",
            BlockEntityType.Builder.of(NetSplitterTile::new, Blocks.NET_SPLITTER));
    public static final BlockEntityType<Print> PRINT = register("Print",
            BlockEntityType.Builder.of(PrintFabric::new, Blocks.PRINT, Blocks.BEACON_BASE_PRINT));
    public static final BlockEntityType<PowerConverter> POWER_CONVERTER = register("PowerConverter",
            BlockEntityType.Builder.of(PowerConverter::new, Blocks.POWER_CONVERTER));
    public static final BlockEntityType<PowerDistributor> POWER_DISTRIBUTOR = register("PowerDistributor",
            BlockEntityType.Builder.of(PowerDistributor::new, Blocks.POWER_DISTRIBUTOR));
    public static final BlockEntityType<Printer> PRINTER = register("Printer",
            BlockEntityType.Builder.of(Printer::new, Blocks.PRINTER));
    public static final BlockEntityType<Rack> RACK = register("Rack",
            BlockEntityType.Builder.of(Rack::new, Blocks.RACK));
    public static final BlockEntityType<Raid> RAID = register("Raid",
            BlockEntityType.Builder.of(Raid::new, Blocks.RAID));
    public static final BlockEntityType<Redstone> REDSTONE = register("Redstone",
            BlockEntityType.Builder.of(RedstoneTile::new, Blocks.REDSTONE));
    public static final BlockEntityType<Screen> SCREEN = register("Screen",
            BlockEntityType.Builder.of((pos, state) -> new ScreenTile(pos, state, 0),
                    Blocks.SCREEN_TIER_1, Blocks.SCREEN_TIER_2, Blocks.SCREEN_TIER_3));
    public static final BlockEntityType<Waypoint> WAYPOINT = register("Waypoint",
            BlockEntityType.Builder.of(Waypoint::new, Blocks.WAYPOINT));
    public static final BlockEntityType<li.cil.oc.core.impl.common.blockentity.Relay> RELAY = register("Relay",
            BlockEntityType.Builder.of(li.cil.oc.core.impl.common.blockentity.Relay::new, Blocks.RELAY));
    public static final BlockEntityType<RobotProxy> ROBOT = register("Robot",
            BlockEntityType.Builder.of(RobotProxy::new, Blocks.ROBOT));
    public static final BlockEntityType<Transposer> TRANSPOSER = register("Transposer",
            BlockEntityType.Builder.of(Transposer::new, Blocks.TRANSPOSER));

    public static void init() {
        Adapter.TYPE = ADAPTER;
        li.cil.oc.core.impl.common.block.Adapter.TYPE = ADAPTER;
        Assembler.TYPE = ASSEMBLER;
        li.cil.oc.core.impl.common.block.Assembler.TYPE = ASSEMBLER;
        Cable.TYPE = CABLE;
        Capacitor.TYPE = CAPACITOR;
        Case.TYPE = CASE;
        Charger.TYPE = CHARGER;
        li.cil.oc.core.impl.common.block.Charger.TYPE = CHARGER;
        Disassembler.TYPE = DISASSEMBLER;
        li.cil.oc.core.impl.common.block.Disassembler.TYPE = DISASSEMBLER;
        DiskDrive.TYPE = DISK_DRIVE;
        Geolyzer.TYPE = GEOLYZER;
        Hologram.TYPE = HOLOGRAM;
        li.cil.oc.core.impl.common.block.Hologram.TYPE = HOLOGRAM;
        Keyboard.TYPE = KEYBOARD;
        Microcontroller.TYPE = MICROCONTROLLER;
        li.cil.oc.core.impl.common.block.Microcontroller.TYPE = MICROCONTROLLER;
        MotionSensor.TYPE = MOTION_SENSOR;
        li.cil.oc.core.impl.common.block.MotionSensor.TYPE = MOTION_SENSOR;
        NetSplitter.TYPE = NET_SPLITTER;
        PowerConverter.TYPE = POWER_CONVERTER;
        li.cil.oc.core.impl.common.block.PowerConverter.TYPE = POWER_CONVERTER;
        PowerDistributor.TYPE = POWER_DISTRIBUTOR;
        li.cil.oc.core.impl.common.block.PowerDistributor.TYPE = POWER_DISTRIBUTOR;
        Print.TYPE = PRINT;
        Printer.TYPE = PRINTER;
        li.cil.oc.core.impl.common.block.Printer.TYPE = PRINTER;
        Rack.TYPE = RACK;
        li.cil.oc.core.impl.common.block.Rack.TYPE = RACK;
        Raid.TYPE = RAID;
        Redstone.TYPE = REDSTONE;
        li.cil.oc.core.impl.common.block.Redstone.TYPE = REDSTONE;
        Screen.TYPE = SCREEN;
        li.cil.oc.core.impl.common.block.Screen.TYPE = SCREEN;
        Waypoint.TYPE = WAYPOINT;
        li.cil.oc.core.impl.common.block.Waypoint.TYPE = WAYPOINT;
        CarpetedCapacitor.TYPE = CARPETED_CAPACITOR;
        li.cil.oc.core.impl.common.block.CarpetedCapacitor.TYPE = CARPETED_CAPACITOR;
        li.cil.oc.core.impl.common.blockentity.Relay.TYPE = RELAY;
    }
}
