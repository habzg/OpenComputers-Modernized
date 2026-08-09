package li.cil.oc.fabric.client;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.renderer.entity.DroneRenderer;
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
import li.cil.oc.core.impl.common.block.ChameliumBlock;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ItemColorizer;
import li.cil.oc.fabric.client.renderer.HighlightRenderer;
import li.cil.oc.fabric.client.renderer.HologramDeferredEventHandler;
import li.cil.oc.fabric.client.renderer.MFUTargetRenderer;
import li.cil.oc.fabric.client.renderer.PetRenderer;
import li.cil.oc.fabric.client.renderer.WirelessNetworkDebugRenderer;
import li.cil.oc.fabric.client.renderer.entity.HoverBootArmorRenderer;
import li.cil.oc.fabric.client.renderer.entity.HoverBootLayer;
import li.cil.oc.fabric.client.renderer.blockentity.CableRenderer;
import li.cil.oc.fabric.client.renderer.blockentity.HologramRenderer;
import li.cil.oc.fabric.client.renderer.blockentity.RobotRenderer;
import li.cil.oc.fabric.client.renderer.blockentity.ScreenRenderer;
import li.cil.oc.fabric.common.event.RackMountableRenderHandler;
import li.cil.oc.fabric.common.init.Blocks;
import li.cil.oc.fabric.common.init.Entities;
import li.cil.oc.fabric.common.init.Items;
import li.cil.oc.fabric.common.init.BlockEntities;
import li.cil.oc.fabric.common.network.OCPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

@SuppressWarnings("unused")
public final class ClientProxy implements ClientModInitializer {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onInitializeClient() {
        li.cil.oc.core.impl.util.Tooltip.setExtendedTooltips(KeyBindings::showExtendedTooltips);
        li.cil.oc.core.impl.common.item.Drone.setExtendedTooltips(KeyBindings::showExtendedTooltips);
        li.cil.oc.core.impl.common.block.PowerConverter.setFabric(true);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> li.cil.oc.core.impl.util.Tooltip.setFont(client.font));
        li.cil.oc.core.impl.client.renderer.gui.BufferRenderer.setProvider(li.cil.oc.fabric.client.renderer.BufferRenderProvider.INSTANCE);
        li.cil.oc.fabric.common.event.FileSystemAccessHandler.setClientLevelChecker(level -> level instanceof net.minecraft.client.multiplayer.ClientLevel);

        li.cil.oc.core.impl.client.gui.Icons.init();
        li.cil.oc.fabric.server.machine.Machine.setGamePausedCheck(() -> Minecraft.getInstance().isPaused());

        BlockEntityRenderers.register(BlockEntities.ADAPTER, AdapterRenderer::new);
        BlockEntityRenderers.register(BlockEntities.ASSEMBLER, AssemblerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.CASE, CaseRenderer::new);
        BlockEntityRenderers.register(BlockEntities.CHARGER, ChargerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.DISASSEMBLER, DisassemblerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.DISK_DRIVE, DiskDriveRenderer::new);
        BlockEntityRenderers.register(BlockEntities.GEOLYZER, GeolyzerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.MICROCONTROLLER, MicrocontrollerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.NET_SPLITTER, NetSplitterRenderer::new);
        BlockEntityRenderers.register(BlockEntities.POWER_DISTRIBUTOR, PowerDistributorRenderer::new);
        BlockEntityRenderers.register(BlockEntities.PRINTER, PrinterRenderer::new);
        BlockEntityRenderers.register(BlockEntities.RAID, RaidRenderer::new);
        BlockEntityRenderers.register(BlockEntities.RELAY, RelayRenderer::new);
        BlockEntityRenderers.register(BlockEntities.TRANSPOSER, TransposerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.RACK, li.cil.oc.fabric.client.renderer.blockentity.RackRenderer::new);
        BlockEntityRenderers.register(BlockEntities.SCREEN, ScreenRenderer::new);
        BlockEntityRenderers.register(BlockEntities.HOLOGRAM, HologramRenderer::new);
        BlockEntityRenderers.register(BlockEntities.CABLE, CableRenderer::new);
        BlockEntityRenderers.register(BlockEntities.ROBOT, RobotRenderer::new);
        EntityRendererRegistry.register(Entities.DRONE, DroneRenderer::new);

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer && livingRenderer.getModel() instanceof net.minecraft.client.model.PlayerModel) {
                registrationHelper.register(new HoverBootLayer((net.minecraft.client.renderer.entity.RenderLayerParent) entityRenderer));
            }
        });

        ArmorRenderer.register(HoverBootArmorRenderer.INSTANCE, Items.HOVER_BOOTS);

        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                BlockEntity te = level.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.traits.Colored colored) {
                    return colored.color();
                }
            }
            return 0xFFFFFF;
        }, Blocks.CASE_TIER_1, Blocks.CASE_TIER_2, Blocks.CASE_TIER_3, Blocks.CASE_CREATIVE,
                Blocks.SCREEN_TIER_1, Blocks.SCREEN_TIER_2, Blocks.SCREEN_TIER_3, Blocks.CABLE);

        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> {
            var dye = state.getValue(ChameliumBlock.COLOR);
            return Color.byMeta(dye.getId());
        }, Blocks.CHAMELIUM_BLOCK);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (ItemColorizer.hasColor(stack)) return 0xFF000000 | ItemColorizer.getColor(stack);
            Block block = Block.byItem(stack.getItem());
            if (block instanceof li.cil.oc.fabric.common.block.Case bc) {
                int t = Math.min(bc.tier, 3);
                return 0xFF000000 | Color.byTier[Math.max(0, t)];
            }
            if (block instanceof li.cil.oc.core.impl.common.block.Screen bs) {
                int t = Math.min(bs.tier, 3);
                return 0xFF000000 | Color.byTier[Math.max(0, t)];
            }
            return 0xFFFFFFFF;
        }, Blocks.CASE_TIER_1, Blocks.CASE_TIER_2, Blocks.CASE_TIER_3, Blocks.CASE_CREATIVE,
                Blocks.SCREEN_TIER_1, Blocks.SCREEN_TIER_2, Blocks.SCREEN_TIER_3);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (ItemColorizer.hasColor(stack)) return 0xFF000000 | ItemColorizer.getColor(stack);
            return 0xFFFFFFFF;
        }, Blocks.CABLE);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            var dye = ChameliumBlock.readDyeColor(stack);
            return 0xFF000000 | (dye != null ? Color.byMeta(dye.getId()) : Color.Black);
        }, Blocks.CHAMELIUM_BLOCK);

        ColorProviderRegistry.ITEM.register(PrintItemColors::getColor, Blocks.PRINT, Blocks.BEACON_BASE_PRINT);

        ClientPlayNetworking.registerGlobalReceiver(OCPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientPacketHandler.INSTANCE.onPacket(io.netty.buffer.Unpooled.wrappedBuffer(payload.data()))));

        Sound.init();

        ClientEventHandler.init();

        HighlightRenderer.init();
        HologramDeferredEventHandler.init();
        MFUTargetRenderer.init();
        PetRenderer.init();
        WirelessNetworkDebugRenderer.init();
        li.cil.oc.fabric.client.renderer.item.UpgradeRenderers.register();

        ModelRegistry.init();

        GuiHandler.registerScreens();

        li.cil.oc.core.impl.common.block.Screen.setOpenGui(pos ->
                GuiHandler.openScreen(GuiType.Screen, pos.getX(), pos.getY(), pos.getZ()));
        li.cil.oc.core.impl.common.block.Waypoint.setOpenGui(pos ->
                GuiHandler.openScreen(GuiType.Waypoint, pos.getX(), pos.getY(), pos.getZ()));

        li.cil.oc.fabric.common.blockentity.Robot.setClientDisposeCallback(robot -> {
            if (Minecraft.getInstance().screen instanceof li.cil.oc.fabric.client.gui.Robot rg && rg.robot == robot) {
                var level = robot.getLevel();
                var address = robot.computerAddress();
                if (level == null || address == null) {
                    Minecraft.getInstance().setScreen(null);
                    return;
                }
                li.cil.oc.core.impl.util.ClientTickScheduler.schedule(() -> {
                    if (Minecraft.getInstance().screen instanceof li.cil.oc.fabric.client.gui.Robot rg2 && rg2.robot == robot
                        && li.cil.oc.core.impl.common.container.RobotLookup.get(level, address) == null) {
                        Minecraft.getInstance().setScreen(null);
                    }
                });
            }
        });

        li.cil.oc.core.impl.common.item.Tablet.setTabletAudioPlayer((player) ->
                li.cil.oc.fabric.util.Audio.play(player.getX(), player.getY() + 2, player.getZ(), "."));

        li.cil.oc.api.API.manual = li.cil.oc.fabric.client.Manual.INSTANCE;

        li.cil.oc.api.Manual.addProvider(new li.cil.oc.fabric.integration.opencomputers.ModOpenComputers.DefinitionPathProvider());
        li.cil.oc.api.Manual.addProvider(new li.cil.oc.api.prefab.ResourceContentProvider(OCSettings.resourceDomain, "doc/"));
        li.cil.oc.api.Manual.addProvider("", new li.cil.oc.core.impl.client.renderer.markdown.segment.render.TextureImageProvider());
        li.cil.oc.api.Manual.addProvider("item", new li.cil.oc.core.impl.client.renderer.markdown.segment.render.ItemImageProvider());
        li.cil.oc.api.Manual.addProvider("block", new li.cil.oc.core.impl.client.renderer.markdown.segment.render.BlockImageProvider());
        li.cil.oc.api.Manual.addProvider("tag", new li.cil.oc.core.impl.client.renderer.markdown.segment.render.OreDictImageProvider());
        li.cil.oc.api.Manual.addTab(new li.cil.oc.api.prefab.TextureTabIconRenderer(li.cil.oc.core.impl.client.Textures.guiManualHome), "gui.opencomputers.manual.home", "%LANGUAGE%/index.md");
        li.cil.oc.api.Manual.addTab(new li.cil.oc.api.prefab.ItemStackTabIconRenderer(li.cil.oc.api.Items.get("case1").createItemStack(1)), "gui.opencomputers.manual.blocks", "%LANGUAGE%/block/index.md");
        li.cil.oc.api.Manual.addTab(new li.cil.oc.api.prefab.ItemStackTabIconRenderer(li.cil.oc.api.Items.get("cpu1").createItemStack(1)), "gui.opencomputers.manual.items", "%LANGUAGE%/item/index.md");

        KeyBindings.register();
        li.cil.oc.core.impl.client.gui.traits.InputBuffer.ClipboardPaste.set(() -> KeyBindings.clipboardPaste.getDefaultKey().getValue());

        li.cil.oc.core.impl.common.item.TexturePicker.setParticleIconProvider((state) -> {
            var mc = Minecraft.getInstance();
            var model = mc.getBlockRenderer().getBlockModel(state);
            var sprite = model.getParticleIcon();
            return sprite.contents().name().toString();
        });

        li.cil.oc.fabric.common.event.NanomachinesHandler.initClient();
        li.cil.oc.fabric.common.EventHandler.initClient();
        li.cil.oc.fabric.client.CommandHandler.init();

        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (chunk instanceof LevelChunk levelChunk) {
                for (var be : levelChunk.getBlockEntities().values()) {
                    if (be instanceof li.cil.oc.api.internal.Rack rack) {
                        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                            if (rack.getMountable(slot) instanceof li.cil.oc.core.impl.common.component.TerminalServer terminal) {
                                var buffer = terminal.bufferIfLoaded();
                                if (buffer != null) {
                                    li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.remove(rack.level(), buffer);
                                    if (buffer instanceof li.cil.oc.core.impl.common.component.TextBuffer concreteBuffer) {
                                        li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.remove(concreteBuffer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        li.cil.oc.api.event.RackMountableRenderEvent.BlockEntity.EVENT.register(RackMountableRenderHandler::onRackMountableRendering);
        li.cil.oc.api.event.RackMountableRenderEvent.Block.EVENT.register(RackMountableRenderHandler::onRackMountableRendering);
    }
}
