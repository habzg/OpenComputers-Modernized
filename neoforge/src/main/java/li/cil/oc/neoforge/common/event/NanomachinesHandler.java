package li.cil.oc.neoforge.common.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.nanomachines.ControllerImpl;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@SuppressWarnings("unused")
public final class NanomachinesHandler {
    public static final class Client {
        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && !mc.isPaused()) {
                Controller ctrl = li.cil.oc.api.Nanomachines.getController(mc.player);
                if (ctrl instanceof ControllerImpl controller && controller.player == mc.player) {
                    controller.update();
                }
            }
        }

        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            Controller controller = li.cil.oc.api.Nanomachines.getController(mc.player);
            if (controller == null) return;
            var guiGraphics = e.getGuiGraphics();
            int sizeX = 8;
            int sizeY = 12;
            int width = guiGraphics.guiWidth();
            int height = guiGraphics.guiHeight();
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
            guiGraphics.blit(Textures.overlayNanomachines, left, top, 0, 0f, 0f, sizeX, sizeY, sizeX, sizeY);
            int barHeight = (int) Math.round(sizeY * fill);
            if (barHeight > 0) {
                guiGraphics.blit(Textures.overlayNanomachinesBar, left, top + sizeY - barHeight, 0, 0f, (float) (sizeY - barHeight), sizeX, barHeight, sizeX, sizeY);
            }
        }
    }

    public static final class Common {
        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
            Controller controller = li.cil.oc.api.Nanomachines.getController(e.getEntity());
            if (controller != null) {
                controller.changeBuffer(-controller.getLocalBuffer());
            }
        }

        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onLivingTick(PlayerTickEvent.Post e) {
            Player player = e.getEntity();
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

        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onPlayerSave(PlayerEvent.SaveToFile e) {
            File file = e.getPlayerFile("ocnm");
            Controller controller = li.cil.oc.api.Nanomachines.getController(e.getEntity());
            if (controller instanceof ControllerImpl impl) {
                try {
                    CompoundTag nbt = new CompoundTag();
                    impl.save(nbt, e.getEntity().level().registryAccess());
                    FileOutputStream fos = new FileOutputStream(file);
                    try {
                        NbtIo.writeCompressed(nbt, fos);
                    } catch (Throwable t) {
                        OpenComputers.log().warn("Error saving nanomachine state.", t);
                    }
                    fos.close();
                } catch (Throwable t) {
                    OpenComputers.log().warn("Error saving nanomachine state.", t);
                }
            }
        }

        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onPlayerLoad(PlayerEvent.LoadFromFile e) {
            File file = e.getPlayerFile("ocnm");
            if (file.exists()) {
                Controller controller = li.cil.oc.api.Nanomachines.getController(e.getEntity());
                if (controller instanceof ControllerImpl impl) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            impl.load(NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.create(0x200000L)), e.getEntity().level().registryAccess());
                        } catch (Throwable t) {
                            OpenComputers.log().warn("Error loading nanomachine state.", t);
                        }
                        fis.close();
                    } catch (Throwable t) {
                        OpenComputers.log().warn("Error loading nanomachine state.", t);
                    }
                }
            }
        }

        @SuppressWarnings("unused")
        @SubscribeEvent
        public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent e) {
            Controller controller = li.cil.oc.api.Nanomachines.getController(e.getEntity());
            if (controller instanceof ControllerImpl) {
                EventHandler.scheduleServer(() -> li.cil.oc.api.Nanomachines.uninstallController(e.getEntity()));
            }
        }
    }
}
