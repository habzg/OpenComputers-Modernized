package li.cil.oc.neoforge.common.init;

import java.util.Locale;
import li.cil.oc.core.impl.common.blockentity.Adapter;
import li.cil.oc.core.impl.common.blockentity.Assembler;
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
import li.cil.oc.core.impl.common.blockentity.Printer;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.core.impl.common.blockentity.Raid;
import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.blockentity.RobotProxy;
import li.cil.oc.neoforge.common.blockentity.CaseTile;
import li.cil.oc.neoforge.common.blockentity.Transposer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("DataFlowIssue")
public final class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.ID);

    private BlockEntities() {
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Adapter>> ADAPTER =
            BLOCK_ENTITY_TYPES.register(key("Adapter"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Adapter::new, Blocks.ADAPTER.get()).build(null);
                        Adapter.TYPE = built;
                        li.cil.oc.core.impl.common.block.Adapter.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Assembler>> ASSEMBLER =
            BLOCK_ENTITY_TYPES.register(key("Assembler"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Assembler::new, Blocks.ASSEMBLER.get()).build(null);
                        Assembler.TYPE = built;
                        li.cil.oc.core.impl.common.block.Assembler.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<li.cil.oc.core.impl.common.blockentity.Cable>> CABLE =
            BLOCK_ENTITY_TYPES.register(key("Cable"),
                    () -> {
                        BlockEntityType<li.cil.oc.core.impl.common.blockentity.Cable> built = BlockEntityType.Builder.of(li.cil.oc.core.impl.common.blockentity.Cable::new, Blocks.CABLE.get()).build(null);
                        li.cil.oc.core.impl.common.blockentity.Cable.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Capacitor>> CAPACITOR =
            BLOCK_ENTITY_TYPES.register(key("Capacitor"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Capacitor::new, Blocks.CAPACITOR.get()).build(null);
                        Capacitor.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR =
            BLOCK_ENTITY_TYPES.register(key("CarpetedCapacitor"),
                    () -> {
                        var built = BlockEntityType.Builder.of(CarpetedCapacitor::new, Blocks.CARPETED_CAPACITOR.get()).build(null);
                        CarpetedCapacitor.TYPE = built;
                        li.cil.oc.core.impl.common.block.CarpetedCapacitor.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Case>> CASE =
            BLOCK_ENTITY_TYPES.register(key("Case"),
                    () -> {
                        var built = BlockEntityType.Builder.<Case>of(
                                (pos, state) -> new CaseTile(pos, state, 0),
                                Blocks.CASE_TIER_1.get(), Blocks.CASE_TIER_2.get(),
                                Blocks.CASE_TIER_3.get(), Blocks.CASE_CREATIVE.get()
                        ).build(null);
                        Case.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Disassembler>> DISASSEMBLER =
            BLOCK_ENTITY_TYPES.register(key("Disassembler"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Disassembler::new, Blocks.DISASSEMBLER.get()).build(null);
                        Disassembler.TYPE = built;
                        li.cil.oc.core.impl.common.block.Disassembler.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDrive>> DISK_DRIVE =
            BLOCK_ENTITY_TYPES.register(key("DiskDrive"),
                    () -> {
                        var built = BlockEntityType.Builder.of(DiskDrive::new, Blocks.DISK_DRIVE.get()).build(null);
                        DiskDrive.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Hologram>> HOLOGRAM =
            BLOCK_ENTITY_TYPES.register(key("Hologram"),
                    () -> {
                        var built = BlockEntityType.Builder.of(
                                Hologram::new,
                                Blocks.HOLOGRAM_TIER_1.get(), Blocks.HOLOGRAM_TIER_2.get()
                        ).build(null);
                        Hologram.TYPE = built;
                        li.cil.oc.core.impl.common.block.Hologram.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Keyboard>> KEYBOARD =
            BLOCK_ENTITY_TYPES.register(key("Keyboard"),
                    () -> {
                        var built = BlockEntityType.Builder.of(
                                Keyboard::new, Blocks.KEYBOARD.get()
                        ).build(null);
                        Keyboard.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Microcontroller>> MICROCONTROLLER =
            BLOCK_ENTITY_TYPES.register(key("Microcontroller"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Microcontroller::new, Blocks.MICROCONTROLLER.get()).build(null);
                        Microcontroller.TYPE = built;
                        li.cil.oc.core.impl.common.block.Microcontroller.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetSplitter>> NET_SPLITTER =
            BLOCK_ENTITY_TYPES.register(key("NetSplitter"),
                    () -> {
                        var built = BlockEntityType.Builder.of(NetSplitter::new, Blocks.NET_SPLITTER.get()).build(null);
                        NetSplitter.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerConverter>> POWER_CONVERTER =
            BLOCK_ENTITY_TYPES.register(key("PowerConverter"),
                    () -> {
                        var built = BlockEntityType.Builder.of(PowerConverter::new, Blocks.POWER_CONVERTER.get()).build(null);
                        PowerConverter.TYPE = built;
                        li.cil.oc.core.impl.common.block.PowerConverter.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR =
            BLOCK_ENTITY_TYPES.register(key("PowerDistributor"),
                    () -> {
                        var built = BlockEntityType.Builder.of(PowerDistributor::new, Blocks.POWER_DISTRIBUTOR.get()).build(null);
                        PowerDistributor.TYPE = built;
                        li.cil.oc.core.impl.common.block.PowerDistributor.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Printer>> PRINTER =
            BLOCK_ENTITY_TYPES.register(key("Printer"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Printer::new, Blocks.PRINTER.get()).build(null);
                        Printer.TYPE = built;
                        li.cil.oc.core.impl.common.block.Printer.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<li.cil.oc.core.impl.common.blockentity.Screen>> SCREEN =
            BLOCK_ENTITY_TYPES.register(key("Screen"),
                    () -> {
                        BlockEntityType<li.cil.oc.core.impl.common.blockentity.Screen> built = BlockEntityType.Builder.<li.cil.oc.core.impl.common.blockentity.Screen>of(
                                (BlockPos pos, BlockState state) -> new li.cil.oc.neoforge.common.blockentity.ScreenTile(pos, state, 0),
                                Blocks.SCREEN_TIER_1.get(), Blocks.SCREEN_TIER_2.get(), Blocks.SCREEN_TIER_3.get()
                        ).build(null);
                        li.cil.oc.core.impl.common.blockentity.Screen.TYPE = built;
                        li.cil.oc.core.impl.common.block.Screen.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Charger>> CHARGER =
            BLOCK_ENTITY_TYPES.register(key("Charger"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Charger::new, Blocks.CHARGER.get()).build(null);
                        Charger.TYPE = built;
                        li.cil.oc.core.impl.common.block.Charger.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Geolyzer>> GEOLYZER =
            BLOCK_ENTITY_TYPES.register(key("Geolyzer"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Geolyzer::new, Blocks.GEOLYZER.get()).build(null);
                        Geolyzer.TYPE = built;
                        return built;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionSensor>> MOTION_SENSOR =
            BLOCK_ENTITY_TYPES.register(key("MotionSensor"),
                    () -> {
                        BlockEntityType<MotionSensor> type = BlockEntityType.Builder.of(MotionSensor::new, Blocks.MOTION_SENSOR.get()).build(null);
                        MotionSensor.TYPE = type;
                        li.cil.oc.core.impl.common.block.MotionSensor.TYPE = type;
                        return type;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<li.cil.oc.core.impl.common.blockentity.Print>> PRINT =
            BLOCK_ENTITY_TYPES.register("print",
                    () -> {
                        BlockEntityType<li.cil.oc.core.impl.common.blockentity.Print> type = BlockEntityType.Builder.of((BlockPos pos, BlockState state) -> (li.cil.oc.core.impl.common.blockentity.Print) new li.cil.oc.neoforge.common.blockentity.PrintNeoForge(pos, state), Blocks.PRINT.get(), Blocks.BEACON_BASE_PRINT.get()).build(null);
                        li.cil.oc.core.impl.common.blockentity.Print.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Rack>> RACK =
            BLOCK_ENTITY_TYPES.register(key("Rack"),
                    () -> {
                        BlockEntityType<Rack> type = BlockEntityType.Builder.of(Rack::new, Blocks.RACK.get()).build(null);
                        Rack.TYPE = type;
                        li.cil.oc.core.impl.common.block.Rack.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Raid>> RAID =
            BLOCK_ENTITY_TYPES.register(key("Raid"),
                    () -> {
                        BlockEntityType<Raid> type = BlockEntityType.Builder.of(Raid::new, Blocks.RAID.get()).build(null);
                        Raid.TYPE = type;
                        return type;
                    });
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<li.cil.oc.core.impl.common.blockentity.Redstone>> REDSTONE =
            BLOCK_ENTITY_TYPES.register(key("Redstone"),
                    () -> {
                        BlockEntityType<li.cil.oc.core.impl.common.blockentity.Redstone> type = BlockEntityType.Builder.<li.cil.oc.core.impl.common.blockentity.Redstone>of(li.cil.oc.neoforge.common.blockentity.RedstoneTile::new, Blocks.REDSTONE.get()).build(null);
                        li.cil.oc.core.impl.common.blockentity.Redstone.TYPE = type;
                        li.cil.oc.core.impl.common.block.Redstone.TYPE = type;
                        return type;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<li.cil.oc.core.impl.common.blockentity.Relay>> RELAY =
            BLOCK_ENTITY_TYPES.register(key("Relay"),
                    () -> {
                        var built = BlockEntityType.Builder.of(li.cil.oc.core.impl.common.blockentity.Relay::new, Blocks.RELAY.get()).build(null);
                        li.cil.oc.core.impl.common.blockentity.Relay.TYPE = built;
                        return built;
                    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobotProxy>> ROBOT =
            BLOCK_ENTITY_TYPES.register(key("Robot"),
                    () -> BlockEntityType.Builder.of(RobotProxy::new, Blocks.ROBOT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Transposer>> TRANSPOSER =
            BLOCK_ENTITY_TYPES.register(key("Transposer"),
                    () -> BlockEntityType.Builder.of(Transposer::new, Blocks.TRANSPOSER.get()).build(null));
    @SuppressWarnings("unused")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Waypoint>> WAYPOINT =
            BLOCK_ENTITY_TYPES.register(key("Waypoint"),
                    () -> {
                        var built = BlockEntityType.Builder.of(Waypoint::new, Blocks.WAYPOINT.get()).build(null);
                        Waypoint.TYPE = built;
                        li.cil.oc.core.impl.common.block.Waypoint.TYPE = built;
                        return built;
                    });
}
