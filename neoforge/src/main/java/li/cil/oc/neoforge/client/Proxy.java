package li.cil.oc.neoforge.client;

import li.cil.oc.core.impl.client.renderer.entity.DroneRenderer;
import li.cil.oc.core.impl.client.renderer.item.HoverBootRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.AdapterRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.AssemblerRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.CaseRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.ChargerRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.DisassemblerRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.DiskDriveRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.GeolyzerRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.MicrocontrollerRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.NetSplitterRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.PowerDistributorRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.PrinterRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.RaidRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.RelayRenderer;
import li.cil.oc.core.impl.client.renderer.blockentity.TransposerRenderer;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ItemColorizer;
import li.cil.oc.neoforge.client.renderer.HighlightRenderer;
import li.cil.oc.neoforge.client.renderer.HologramDeferredEventHandler;
import li.cil.oc.neoforge.client.renderer.MFUTargetRenderer;
import li.cil.oc.neoforge.client.renderer.PetRenderer;
import li.cil.oc.neoforge.client.renderer.WirelessNetworkDebugRenderer;
import li.cil.oc.neoforge.client.renderer.entity.HoverBootLayer;
import li.cil.oc.neoforge.client.renderer.blockentity.CableRenderer;
import li.cil.oc.neoforge.client.renderer.blockentity.HologramRenderer;
import li.cil.oc.neoforge.client.renderer.blockentity.RackRenderer;
import li.cil.oc.neoforge.client.renderer.blockentity.RobotRenderer;
import li.cil.oc.neoforge.client.renderer.blockentity.ScreenRenderer;
import li.cil.oc.neoforge.common.event.NanomachinesHandler;
import li.cil.oc.neoforge.common.event.RackMountableRenderHandler;
import li.cil.oc.neoforge.common.init.Blocks;
import li.cil.oc.neoforge.common.init.Entities;
import li.cil.oc.neoforge.common.init.Items;
import li.cil.oc.neoforge.common.init.BlockEntities;
import li.cil.oc.neoforge.model.CableModel;
import li.cil.oc.neoforge.model.DroneModel;
import li.cil.oc.neoforge.model.FloppyModel;
import li.cil.oc.neoforge.model.NetSplitterModel;
import li.cil.oc.neoforge.model.RobotModel;
import li.cil.oc.neoforge.model.ScreenModel;
import li.cil.oc.neoforge.util.Audio;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

public class Proxy extends li.cil.oc.neoforge.common.Proxy {
    @Override
    @SuppressWarnings("unused")
    public void preInit(FMLCommonSetupEvent e) {
        super.preInit(e);
        li.cil.oc.api.API.manual = li.cil.oc.neoforge.client.Manual.INSTANCE;
    }

    @Override
    @SuppressWarnings("unused")
    public void init(FMLCommonSetupEvent e) {
        super.init(e);

        li.cil.oc.core.impl.client.gui.Icons.init();

        NeoForge.EVENT_BUS.register(CommandHandler.class);
        NeoForge.EVENT_BUS.register(HighlightRenderer.class);
        NeoForge.EVENT_BUS.register(NanomachinesHandler.Client.class);
        NeoForge.EVENT_BUS.register(PetRenderer.class);
        NeoForge.EVENT_BUS.register(RackMountableRenderHandler.class);
        NeoForge.EVENT_BUS.register(li.cil.oc.neoforge.client.Sound.class);
        NeoForge.EVENT_BUS.register(MFUTargetRenderer.class);
        NeoForge.EVENT_BUS.register(WirelessNetworkDebugRenderer.class);
        NeoForge.EVENT_BUS.register(HologramDeferredEventHandler.class);
        NeoForge.EVENT_BUS.register(Audio.class);
    }

    public static void handleRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (var dyeName : Color.dyes) {
            event.register(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath("opencomputers", "item/floppy_" + dyeName.toLowerCase()), "standalone"));
        }
    }

    public static void handleModelBake(ModelEvent.ModifyBakingResult event) {
        var printBlockId = ResourceLocation.fromNamespaceAndPath("opencomputers", "print");
        event.getModels().replaceAll((key, model) -> {
            if (key.id().equals(printBlockId)) {
                return new li.cil.oc.neoforge.model.PrintModel(model);
            }
            return model;
        });

        var printItemKey = new ModelResourceLocation(Items.PRINT.getId(), "inventory");
        var printItemModel = event.getModels().get(printItemKey);
        if (printItemModel != null) {
            event.getModels().put(printItemKey, new li.cil.oc.neoforge.model.PrintModel(printItemModel));
        }

        var beaconBlockId = ResourceLocation.fromNamespaceAndPath("opencomputers", "beaconbaseprint");
        event.getModels().replaceAll((key, model) -> {
            if (key.id().equals(beaconBlockId)) {
                return new li.cil.oc.neoforge.model.PrintModel(model);
            }
            return model;
        });

        var beaconItemKey = new ModelResourceLocation(Items.BEACON_BASE_PRINT.getId(), "inventory");
        var beaconItemModel = event.getModels().get(beaconItemKey);
        if (beaconItemModel != null) {
            event.getModels().put(beaconItemKey, new li.cil.oc.neoforge.model.PrintModel(beaconItemModel));
        }

        var splitterBlockId = ResourceLocation.fromNamespaceAndPath("opencomputers", "netsplitter");
        event.getModels().replaceAll((key, model) -> {
            if (key.id().equals(splitterBlockId)) {
                return new NetSplitterModel();
            }
            return model;
        });

        var cableLoc = ResourceLocation.fromNamespaceAndPath("opencomputers", "block/cable");
        var capLoc = ResourceLocation.fromNamespaceAndPath("opencomputers", "block/cablecap");

        var itemKey = new ModelResourceLocation(Items.CABLE.getId(), "inventory");
        var itemModel = event.getModels().get(itemKey);
        if (itemModel != null) {
            event.getModels().put(itemKey, new CableModel(cableLoc, capLoc, itemModel));
        }

        var floppyKey = new ModelResourceLocation(Items.FLOPPY.getId(), "inventory");
        var floppyModel = event.getModels().get(floppyKey);
        if (floppyModel != null) {
            var dyeModels = new BakedModel[16];
            for (int i = 0; i < 16; i++) {
                var dyeName = Color.dyes[i];
                var dyeKey = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath("opencomputers", "item/floppy_" + dyeName.toLowerCase()), "standalone");
                dyeModels[i] = event.getModels().get(dyeKey);
            }
            event.getModels().put(floppyKey, new FloppyModel(dyeModels));
        }

        var robotKey = new ModelResourceLocation(Items.ROBOT.getId(), "inventory");
        var robotModel = event.getModels().get(robotKey);
        if (robotModel != null) {
            event.getModels().put(robotKey, new RobotModel(robotModel));
        }

        var droneKey = new ModelResourceLocation(Items.DRONE.getId(), "inventory");
        var droneModel = event.getModels().get(droneKey);
        if (droneModel != null) {
            event.getModels().put(droneKey, new DroneModel());
        }

        var screen1Key = new ModelResourceLocation(Items.SCREEN_TIER_1.getId(), "inventory");
        event.getModels().put(screen1Key, new ScreenModel());
        var screen2Key = new ModelResourceLocation(Items.SCREEN_TIER_2.getId(), "inventory");
        event.getModels().put(screen2Key, new ScreenModel());
        var screen3Key = new ModelResourceLocation(Items.SCREEN_TIER_3.getId(), "inventory");
        event.getModels().put(screen3Key, new ScreenModel());
    }

    public static void handleRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntities.ADAPTER.get(), AdapterRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.CABLE.get(), CableRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.ASSEMBLER.get(), AssemblerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.CASE.get(), CaseRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.CHARGER.get(), ChargerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.DISASSEMBLER.get(), DisassemblerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.GEOLYZER.get(), GeolyzerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.HOLOGRAM.get(), HologramRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.MICROCONTROLLER.get(), MicrocontrollerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.NET_SPLITTER.get(), NetSplitterRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.POWER_DISTRIBUTOR.get(), PowerDistributorRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.PRINTER.get(), PrinterRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.RACK.get(), RackRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.RAID.get(), RaidRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.RELAY.get(), RelayRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.ROBOT.get(), RobotRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.SCREEN.get(), ScreenRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.TRANSPOSER.get(), TransposerRenderer::new);
        event.registerEntityRenderer(Entities.DRONE.get(), DroneRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void handleRegisterEntityLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new HoverBootLayer(livingRenderer));
            }
        }
    }

    public static void handleRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor teColor = (state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                BlockEntity te = level.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.traits.Colored colored) {
                    return colored.color();
                }
            }
            return 0xFFFFFF;
        };
        event.register(teColor, Blocks.CASE_TIER_1.get());
        event.register(teColor, Blocks.CASE_TIER_2.get());
        event.register(teColor, Blocks.CASE_TIER_3.get());
        event.register(teColor, Blocks.CASE_CREATIVE.get());
        event.register(teColor, Blocks.SCREEN_TIER_1.get());
        event.register(teColor, Blocks.SCREEN_TIER_2.get());
        event.register(teColor, Blocks.SCREEN_TIER_3.get());
        event.register(teColor, Blocks.CABLE.get());

        BlockColor chameliumColor = (state, level, pos, tintIndex) -> {
            net.minecraft.world.item.DyeColor dye = state.getValue(li.cil.oc.core.impl.common.block.ChameliumBlock.COLOR);
            return li.cil.oc.core.impl.util.Color.byMeta(dye.getId());
        };
        event.register(chameliumColor, Blocks.CHAMELIUM_BLOCK.get());
    }

    public static void handleRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(@NotNull LivingEntity entity, @NotNull ItemStack stack, @NotNull EquipmentSlot slot, @NotNull HumanoidModel<?> original) {
                HoverBootRenderer.INSTANCE.lightColor = ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : 0x66DD55;
                return HoverBootRenderer.INSTANCE;
            }
        }, Items.HOVER_BOOTS.get());
    }

    public static void handleRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
                    if (ItemColorizer.hasColor(stack)) return 0xFF000000 | ItemColorizer.getColor(stack);
                    Block block = Block.byItem(stack.getItem());
                    if (block instanceof li.cil.oc.neoforge.common.block.Case bc) {
                        int t = Math.min(bc.tier, 3);
                        return 0xFF000000 | li.cil.oc.core.impl.util.Color.byTier[Math.max(0, t)];
                    }
                    if (block instanceof li.cil.oc.core.impl.common.block.Screen bs) {
                        int t = Math.min(bs.tier, 3);
                        return 0xFF000000 | li.cil.oc.core.impl.util.Color.byTier[Math.max(0, t)];
                    }
                    return 0xFFFFFFFF;
                }, Blocks.CASE_TIER_1.get().asItem(), Blocks.CASE_TIER_2.get().asItem(), Blocks.CASE_TIER_3.get().asItem(), Blocks.CASE_CREATIVE.get().asItem(),
                Blocks.SCREEN_TIER_1.get().asItem(), Blocks.SCREEN_TIER_2.get().asItem(), Blocks.SCREEN_TIER_3.get().asItem());
        event.register((stack, tintIndex) -> {
            if (ItemColorizer.hasColor(stack)) return 0xFF000000 | ItemColorizer.getColor(stack);
            return 0xFFFFFFFF;
        }, Blocks.CABLE.get().asItem());
        event.register((stack, tintIndex) -> {
            var dye = li.cil.oc.core.impl.common.block.ChameliumBlock.readDyeColor(stack);
            return 0xFF000000 | (dye != null ? li.cil.oc.core.impl.util.Color.byMeta(dye.getId()) : li.cil.oc.core.impl.util.Color.Black);
        }, Blocks.CHAMELIUM_BLOCK.get().asItem());
    }
}
