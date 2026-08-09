package li.cil.oc.fabric.common.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.nanomachines.ControllerImpl;
import li.cil.oc.core.impl.util.PlayerUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class NanomachinesHandler {
    public static void init() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> drainBuffer(newPlayer));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Controller ctrl = li.cil.oc.api.Nanomachines.getController(player);
                if (ctrl instanceof ControllerImpl controller) {
                    if (controller.player == player) {
                        controller.update();
                    } else {
                        CompoundTag nbt = new CompoundTag();
                        controller.save(nbt, player.level().registryAccess());
                        li.cil.oc.api.Nanomachines.uninstallController(controller.player);
                        Controller newCtrl = li.cil.oc.api.Nanomachines.installController(player);
                        if (newCtrl instanceof ControllerImpl newController) {
                            newController.load(nbt, player.level().registryAccess());
                            newController.reset();
                        }
                    }
                }
            }

            if (server.getTickCount() % 6000 == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    savePlayerData(player);
                }
            }
        });

        ServerPlayerEvents.JOIN.register(NanomachinesHandler::loadPlayerData);

        ServerPlayerEvents.LEAVE.register(player -> {
            Controller controller = li.cil.oc.api.Nanomachines.getController(player);
            if (controller instanceof ControllerImpl) {
                savePlayerData(player);
                li.cil.oc.api.Nanomachines.uninstallController(player);
                PlayerUtils.persistedData(player).putBoolean(OCSettings.namespace + "hasNanomachines", true);
            }
        });
    }

    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !client.isPaused()) {
                Controller ctrl = li.cil.oc.api.Nanomachines.getController(client.player);
                if (ctrl instanceof ControllerImpl controller && controller.player == client.player) {
                    controller.update();
                }
            }
        });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            Controller controller = li.cil.oc.api.Nanomachines.getController(mc.player);
            if (controller == null) return;
            int sizeX = 8;
            int sizeY = 12;
            int width = drawContext.guiWidth();
            int height = drawContext.guiHeight();
            double[] pos = OCSettings.get().nanomachineHudPos;
            double x = pos[0];
            double y = pos[1];
            double leftValue;
            if (x < 0) {
                leftValue = (double) width / 2 - 91 - 12;
            } else if (x < 1) {
                leftValue = width * x;
            } else {
                leftValue = x;
            }
            int left = (int) Math.min(width - sizeX, leftValue);
            double topValue;
            if (y < 0) {
                topValue = height - 39;
            } else if (y < 1) {
                topValue = y * height;
            } else {
                topValue = y;
            }
            int top = (int) Math.min(height - sizeY, topValue);
            double fill = controller.getLocalBuffer() / controller.getLocalBufferSize();
            drawContext.blit(Textures.overlayNanomachines, left, top, 0, 0f, 0f, sizeX, sizeY, sizeX, sizeY);
            int barHeight = (int) Math.round(sizeY * fill);
            if (barHeight > 0) {
                drawContext.blit(Textures.overlayNanomachinesBar, left, top + sizeY - barHeight, 0, 0f, (float) (sizeY - barHeight), sizeX, barHeight, sizeX, sizeY);
            }
        });
    }

    private static void drainBuffer(ServerPlayer player) {
        Controller controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller != null) {
            controller.changeBuffer(-controller.getLocalBuffer());
        }
    }

    private static void savePlayerData(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return;
        File dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(dir, player.getStringUUID() + ".ocnm");
        Controller controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller instanceof ControllerImpl impl) {
            try {
                CompoundTag nbt = new CompoundTag();
                impl.save(nbt, player.level().registryAccess());
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    NbtIo.writeCompressed(nbt, fos);
                } catch (Throwable t) {
                    li.cil.oc.fabric.OpenComputers.log().warn("Error saving nanomachine state.", t);
                }
                fos.close();
            } catch (Throwable t) {
                li.cil.oc.fabric.OpenComputers.log().warn("Error saving nanomachine state.", t);
            }
        }
    }

    private static void loadPlayerData(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return;
        File dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(dir, player.getStringUUID() + ".ocnm");
        if (file.exists()) {
            Controller controller = li.cil.oc.api.Nanomachines.getController(player);
            if (controller instanceof ControllerImpl impl) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    try {
                        impl.load(NbtIo.readCompressed(fis, NbtAccounter.create(0x200000L)), player.level().registryAccess());
                    } catch (Throwable t) {
                        li.cil.oc.fabric.OpenComputers.log().warn("Error loading nanomachine state.", t);
                    }
                    fis.close();
                } catch (Throwable t) {
                    li.cil.oc.fabric.OpenComputers.log().warn("Error loading nanomachine state.", t);
                }
            }
        }
    }
}
