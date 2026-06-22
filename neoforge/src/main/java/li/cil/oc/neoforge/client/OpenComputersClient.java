package li.cil.oc.neoforge.client;

import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.client.renderer.item.UpgradeRenderers;
import li.cil.oc.neoforge.common.tileentity.Robot;
import li.cil.oc.neoforge.integration.Mods;
import li.cil.oc.neoforge.util.Tooltip;
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

        Robot.setClientDisposeCallback(robot -> {
            if (Minecraft.getInstance().screen instanceof li.cil.oc.neoforge.client.gui.Robot rg && rg.robot == robot)
                Minecraft.getInstance().setScreen(null);
        });

        li.cil.oc.neoforge.common.event.FileSystemAccessHandler.setClientLevelChecker(level -> level instanceof net.minecraft.client.multiplayer.ClientLevel);

        li.cil.oc.neoforge.common.item.Drone.setExtendedTooltips(li.cil.oc.neoforge.client.KeyBindings::showExtendedTooltips);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClientEventHandler.class);

        modEventBus.addListener(FMLClientSetupEvent.class, event -> {
            Tooltip.setFont(Minecraft.getInstance().font);
            Tooltip.setExtendedTooltips(KeyBindings::showExtendedTooltips);
            li.cil.oc.neoforge.server.machine.Machine.setGamePausedCheck(() -> Minecraft.getInstance().isPaused());
            if (Mods.CBMultipart.isModAvailable()) {
                li.cil.oc.neoforge.integration.cbmultipart.ClientMultipartInit.registerRenderers();
            }
        });
    }
}
