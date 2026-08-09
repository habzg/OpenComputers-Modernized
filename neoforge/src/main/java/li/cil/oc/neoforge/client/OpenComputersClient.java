package li.cil.oc.neoforge.client;

import li.cil.oc.core.impl.util.Tooltip;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.client.renderer.item.UpgradeRenderers;
import li.cil.oc.neoforge.common.blockentity.Robot;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = OpenComputers.ID, dist = Dist.CLIENT)
@SuppressWarnings("unused")
public final class OpenComputersClient {
    public OpenComputersClient(IEventBus modEventBus) {
        OpenComputers.proxy = new li.cil.oc.neoforge.client.Proxy();

        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleModelBake);
        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleRegisterAdditionalModels);
        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleRegisterBlockEntityRenderers);
        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleRegisterEntityLayers);
        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleRegisterBlockColors);
        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleRegisterItemColors);
        modEventBus.addListener(li.cil.oc.neoforge.client.Proxy::handleRegisterClientExtensions);

        modEventBus.addListener(li.cil.oc.neoforge.client.GuiHandler::handleRegisterMenuScreens);
        modEventBus.addListener(li.cil.oc.neoforge.client.KeyBindings::registerBindings);

        UpgradeRenderers.register();

        li.cil.oc.core.impl.client.gui.traits.InputBuffer.ClipboardPaste.set(() -> KeyBindings.clipboardPaste.getKey().getValue());

        li.cil.oc.core.impl.common.block.Screen.setOpenGui(pos ->
                li.cil.oc.neoforge.client.GuiHandler.openScreen(li.cil.oc.core.common.GuiType.Screen, pos.getX(), pos.getY(), pos.getZ()));
        li.cil.oc.core.impl.common.block.Waypoint.setOpenGui(pos ->
                li.cil.oc.neoforge.client.GuiHandler.openScreen(li.cil.oc.core.common.GuiType.Waypoint, pos.getX(), pos.getY(), pos.getZ()));

        Robot.setClientDisposeCallback(robot -> {
            if (Minecraft.getInstance().screen instanceof li.cil.oc.neoforge.client.gui.Robot rg && rg.robot == robot) {
                var level = robot.getLevel();
                var address = robot.computerAddress();
                if (level == null || address == null) {
                    Minecraft.getInstance().setScreen(null);
                    return;
                }
                li.cil.oc.core.impl.util.ClientTickScheduler.schedule(() -> {
                    if (Minecraft.getInstance().screen instanceof li.cil.oc.neoforge.client.gui.Robot rg2 && rg2.robot == robot
                        && li.cil.oc.core.impl.common.container.RobotLookup.get(level, address) == null) {
                        Minecraft.getInstance().setScreen(null);
                    }
                });
            }
        });

        li.cil.oc.neoforge.common.event.FileSystemAccessHandler.setClientLevelChecker(level -> level instanceof net.minecraft.client.multiplayer.ClientLevel);

        li.cil.oc.core.impl.common.item.Drone.setExtendedTooltips(li.cil.oc.neoforge.client.KeyBindings::showExtendedTooltips);
        li.cil.oc.core.impl.common.item.Tablet.setTabletAudioPlayer((player) ->
                li.cil.oc.neoforge.util.Audio.play(player.getX(), player.getY() + 2, player.getZ(), "."));
        li.cil.oc.core.impl.common.item.TexturePicker.setParticleIconProvider((state) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var model = mc.getBlockRenderer().getBlockModel(state);
            var sprite = model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
            return sprite.contents().name().toString();
        });

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClientEventHandler.class);

        modEventBus.addListener(FMLClientSetupEvent.class, event -> {
            li.cil.oc.core.impl.client.renderer.gui.BufferRenderer.setProvider(li.cil.oc.neoforge.client.renderer.BufferRenderProvider.INSTANCE);
            li.cil.oc.core.impl.util.PlayerUtils.setDataProvider(li.cil.oc.neoforge.common.PlayerDataProvider.INSTANCE);
            Tooltip.setFont(Minecraft.getInstance().font);
            Tooltip.setExtendedTooltips(KeyBindings::showExtendedTooltips);
            li.cil.oc.neoforge.server.machine.Machine.setGamePausedCheck(() -> Minecraft.getInstance().isPaused());
            if (Mods.CBMultipart.isModAvailable()) {
                li.cil.oc.neoforge.integration.cbmultipart.ClientMultipartInit.registerRenderers();
            }
        });
    }
}
