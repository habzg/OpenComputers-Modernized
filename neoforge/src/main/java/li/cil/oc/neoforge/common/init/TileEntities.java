package li.cil.oc.neoforge.common.init;

import li.cil.oc.core.impl.common.tileentity.Adapter;
import li.cil.oc.core.impl.common.tileentity.Assembler;
import li.cil.oc.core.impl.common.tileentity.Cable;
import li.cil.oc.core.impl.common.tileentity.Capacitor;
import li.cil.oc.core.impl.common.tileentity.CarpetedCapacitor;
import li.cil.oc.core.impl.common.tileentity.Case;
import li.cil.oc.core.impl.common.tileentity.Charger;
import li.cil.oc.core.impl.common.tileentity.Disassembler;
import li.cil.oc.core.impl.common.tileentity.DiskDrive;
import li.cil.oc.core.impl.common.tileentity.Geolyzer;
import li.cil.oc.core.impl.common.tileentity.Hologram;
import li.cil.oc.core.impl.common.tileentity.Keyboard;
import li.cil.oc.core.impl.common.tileentity.Microcontroller;
import li.cil.oc.core.impl.common.tileentity.MotionSensor;
import li.cil.oc.core.impl.common.tileentity.NetSplitter;
import li.cil.oc.core.impl.common.tileentity.PowerConverter;
import li.cil.oc.core.impl.common.tileentity.PowerDistributor;
import li.cil.oc.core.impl.common.tileentity.Printer;
import li.cil.oc.core.impl.common.tileentity.Rack;
import li.cil.oc.core.impl.common.tileentity.Raid;
import li.cil.oc.core.impl.common.tileentity.Redstone;
import li.cil.oc.core.impl.common.tileentity.Screen;
import li.cil.oc.core.impl.common.tileentity.Waypoint;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.tileentity.Relay;
import li.cil.oc.neoforge.common.tileentity.RobotProxy;
import li.cil.oc.neoforge.common.tileentity.Transposer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;

@SuppressWarnings("DataFlowIssue")
public final class TileEntities {
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.ID);

    private TileEntities() {
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Adapter>> ADAPTER =
            TILE_ENTITY_TYPES.register(key("Adapter"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Adapter::new, Blocks.ADAPTER.get()).build(null);
                        Adapter.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Assembler>> ASSEMBLER =
            TILE_ENTITY_TYPES.register(key("Assembler"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Assembler::new, Blocks.ASSEMBLER.get()).build(null);
                        Assembler.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Cable>> CABLE =
            TILE_ENTITY_TYPES.register(key("Cable"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Cable::new, Blocks.CABLE.get()).build(null);
                        Cable.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Capacitor>> CAPACITOR =
            TILE_ENTITY_TYPES.register(key("Capacitor"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Capacitor::new, Blocks.CAPACITOR.get()).build(null);
                        Capacitor.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR =
            TILE_ENTITY_TYPES.register(key("CarpetedCapacitor"),
                    () -> {
                        var built = BlockEntityType.Builder.of(CarpetedCapacitor::new, Blocks.CARPETED_CAPACITOR.get()).build(null);
                        CarpetedCapacitor.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Case>> CASE =
            TILE_ENTITY_TYPES.register(key("Case"),
                    () -> {
                        var built = BlockEntityType.Builder.<Case>of(
                                (pos, state) -> new li.cil.oc.neoforge.common.tileentity.CaseGoggleTileEntity(pos, state, 0),
                                Blocks.CASE_TIER_1.get(), Blocks.CASE_TIER_2.get(),
                                Blocks.CASE_TIER_3.get(), Blocks.CASE_CREATIVE.get()
                        ).build(null);
                        Case.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Disassembler>> DISASSEMBLER =
            TILE_ENTITY_TYPES.register(key("Disassembler"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Disassembler::new, Blocks.DISASSEMBLER.get()).build(null);
                        Disassembler.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDrive>> DISK_DRIVE =
            TILE_ENTITY_TYPES.register(key("DiskDrive"),
                    () -> {
                        var built = BlockEntityType.Builder.of(DiskDrive::new, Blocks.DISK_DRIVE.get()).build(null);
                        DiskDrive.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Hologram>> HOLOGRAM =
            TILE_ENTITY_TYPES.register(key("Hologram"),
                    () -> {
                        var built = BlockEntityType.Builder.of(
                                Hologram::new,
                                Blocks.HOLOGRAM_TIER_1.get(), Blocks.HOLOGRAM_TIER_2.get()
                        ).build(null);
                        Hologram.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Keyboard>> KEYBOARD =
            TILE_ENTITY_TYPES.register(key("Keyboard"),
                    () -> {
                        var built = BlockEntityType.Builder.of(
                                Keyboard::new, Blocks.KEYBOARD.get()
                        ).build(null);
                        Keyboard.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Microcontroller>> MICROCONTROLLER =
            TILE_ENTITY_TYPES.register(key("Microcontroller"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Microcontroller::new, Blocks.MICROCONTROLLER.get()).build(null);
                        Microcontroller.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetSplitter>> NET_SPLITTER =
            TILE_ENTITY_TYPES.register(key("NetSplitter"),
                    () -> {
                        var built = BlockEntityType.Builder.of(NetSplitter::new, Blocks.NET_SPLITTER.get()).build(null);
                        NetSplitter.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerConverter>> POWER_CONVERTER =
            TILE_ENTITY_TYPES.register(key("PowerConverter"),
                    () -> {
                        var built = BlockEntityType.Builder.of(PowerConverter::new, Blocks.POWER_CONVERTER.get()).build(null);
                        PowerConverter.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR =
            TILE_ENTITY_TYPES.register(key("PowerDistributor"),
                    () -> {
                        var built = BlockEntityType.Builder.of(PowerDistributor::new, Blocks.POWER_DISTRIBUTOR.get()).build(null);
                        PowerDistributor.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Printer>> PRINTER =
            TILE_ENTITY_TYPES.register(key("Printer"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Printer::new, Blocks.PRINTER.get()).build(null);
                        Printer.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Screen>> SCREEN =
            TILE_ENTITY_TYPES.register(key("Screen"),
                    () -> {
                        var built = BlockEntityType.Builder.of(
                                Screen::new,
                                Blocks.SCREEN_TIER_1.get(), Blocks.SCREEN_TIER_2.get(), Blocks.SCREEN_TIER_3.get()
                        ).build(null);
                        Screen.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Charger>> CHARGER =
            TILE_ENTITY_TYPES.register(key("Charger"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Charger::new, Blocks.CHARGER.get()).build(null);
                        Charger.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Geolyzer>> GEOLYZER =
            TILE_ENTITY_TYPES.register(key("Geolyzer"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Geolyzer::new, Blocks.GEOLYZER.get()).build(null);
                        Geolyzer.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionSensor>> MOTION_SENSOR =
            TILE_ENTITY_TYPES.register(key("MotionSensor"),
                    () -> {
                        BlockEntityType<MotionSensor> type = BlockEntityType.Builder.of(MotionSensor::new, Blocks.MOTION_SENSOR.get()).build(null);
                        MotionSensor.TYPE = type;
                        return type;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<li.cil.oc.core.impl.common.tileentity.Print>> PRINT =
            TILE_ENTITY_TYPES.register("print",
                    () -> {
                        BlockEntityType<li.cil.oc.core.impl.common.tileentity.Print> type = BlockEntityType.Builder.of((BlockPos pos, BlockState state) -> (li.cil.oc.core.impl.common.tileentity.Print) new li.cil.oc.neoforge.common.tileentity.PrintNeoForge(pos, state), Blocks.PRINT.get(), Blocks.BEACON_BASE_PRINT.get()).build(null);
                        li.cil.oc.core.impl.common.tileentity.Print.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Rack>> RACK =
            TILE_ENTITY_TYPES.register(key("Rack"),
                    () -> {
                        BlockEntityType<Rack> type = BlockEntityType.Builder.of(Rack::new, Blocks.RACK.get()).build(null);
                        Rack.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Raid>> RAID =
            TILE_ENTITY_TYPES.register(key("Raid"),
                    () -> {
                        BlockEntityType<Raid> type = BlockEntityType.Builder.of(Raid::new, Blocks.RAID.get()).build(null);
                        Raid.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Redstone>> REDSTONE =
            TILE_ENTITY_TYPES.register(key("Redstone"),
                    () -> {
                        BlockEntityType<Redstone> type = BlockEntityType.Builder.of(Redstone::new, Blocks.REDSTONE.get()).build(null);
                        Redstone.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Relay>> RELAY =
            TILE_ENTITY_TYPES.register(key("Relay"),
                    () -> BlockEntityType.Builder.of(Relay::new, Blocks.RELAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobotProxy>> ROBOT =
            TILE_ENTITY_TYPES.register(key("Robot"),
                    () -> BlockEntityType.Builder.of(RobotProxy::new, Blocks.ROBOT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Transposer>> TRANSPOSER =
            TILE_ENTITY_TYPES.register(key("Transposer"),
                    () -> BlockEntityType.Builder.of(Transposer::new, Blocks.TRANSPOSER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Waypoint>> WAYPOINT =
            TILE_ENTITY_TYPES.register(key("Waypoint"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Waypoint::new, Blocks.WAYPOINT.get()).build(null);
                        Waypoint.TYPE = built;
                        return built;
                    });
}
