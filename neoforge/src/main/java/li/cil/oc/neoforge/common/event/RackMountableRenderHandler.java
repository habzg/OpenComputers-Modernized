package li.cil.oc.neoforge.common.event;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.core.Constants;
import li.cil.oc.core.client.renderer.blockentity.RenderUtil;
import li.cil.oc.core.impl.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public final class RackMountableRenderHandler {
    private RackMountableRenderHandler() {
    }

    private static final ItemInfo DiskDriveMountable = Items.get(Constants.ItemName.DiskDriveMountable);
    private static final ItemInfo ServerTier1 = Items.get(Constants.ItemName.ServerTier1);
    private static final ItemInfo ServerTier2 = Items.get(Constants.ItemName.ServerTier2);
    private static final ItemInfo ServerTier3 = Items.get(Constants.ItemName.ServerTier3);
    private static final ItemInfo ServerCreative = Items.get(Constants.ItemName.ServerCreative);
    private static final ItemInfo TerminalServer = Items.get(Constants.ItemName.TerminalServer);

    @SubscribeEvent
    public static void onRackMountableRendering(RackMountableRenderEvent.BlockEntity e) {
        var item = Items.get(e.rack.getItem(e.mountable));
        if (item == null) return;

        if (item == DiskDriveMountable) {
            var level = e.rack.level();
            if (level != null) {
                if (e.data != null && e.data.contains("disk")) {
                    var stack = ItemStack.parseOptional(level.registryAccess(), e.data.getCompound("disk"));
                    if (!stack.isEmpty()) {
                        var poseStack = e.getPoseStack();
                        var bufferSource = e.getBufferSource();
                        if (poseStack != null && bufferSource != null) {
                            poseStack.pushPose();
                            poseStack.scale(1, -1, 1);
                            poseStack.translate(10 / 16f, -(3.5f + e.mountable * 3f) / 16f, -2 / 16f);
                            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90F));
                            poseStack.scale(0.5f, 0.5f, 0.5f);
                            Minecraft.getInstance().getItemRenderer().renderStatic(
                                    stack, ItemDisplayContext.FIXED,
                                    e.getPackedLight(), e.getPackedOverlay(),
                                    poseStack, bufferSource, level, 0);
                            poseStack.popPose();
                        }
                    }
                }
            }
            if (e.data != null && System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.level().random.nextDouble() > 0.1) {
                renderOverlay(e, Textures.blockRackDiskDriveActivity);
            }
        } else if (item == ServerTier1 || item == ServerTier2 || item == ServerTier3 || item == ServerCreative) {
            if (e.data == null) return;
            if (e.data.getBoolean("isRunning")) {
                renderOverlay(e, Textures.blockRackServerOn);
            }
            if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode() * (e.mountable + 1))) {
                renderOverlay(e, Textures.blockRackServerError);
            }
            if (System.currentTimeMillis() - e.data.getLong("lastFileSystemAccess") < 400 && e.rack.level().random.nextDouble() > 0.1) {
                renderOverlay(e, Textures.blockRackServerActivity);
            }
            if ((System.currentTimeMillis() - e.data.getLong("lastNetworkActivity") < 300 && System.currentTimeMillis() % 200 > 100) && e.data.getBoolean("isRunning")) {
                renderOverlay(e, Textures.blockRackServerNetworkActivity);
            }
        } else if (item == TerminalServer) {
            if (e.data == null) return;
            renderOverlay(e, Textures.blockRackTerminalServerOn);
            var keys = e.data.getList("keys", Tag.TAG_STRING);
            int countConnected = keys.size();
            if (countConnected > 0) {
                float u0 = 7 / 16f;
                float u1 = u0 + (2 * countConnected - 1) / 16f;
                renderOverlay(e, Textures.blockRackTerminalServerPresence, u0, u1);
            }
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRackMountableRendering(RackMountableRenderEvent.Block e) {
        var item = Items.get(e.rack.getItem(e.mountable));
        if (item == null) return;

        var atlas = Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);

        if (item == DiskDriveMountable) {
            e.setFrontTextureOverride(atlas.getSprite(Textures.blockRackDiskDrive));
        } else if (item == ServerTier1 || item == ServerTier2 || item == ServerTier3 || item == ServerCreative) {
            e.setFrontTextureOverride(atlas.getSprite(Textures.blockRackServer));
        } else if (item == TerminalServer) {
            e.setFrontTextureOverride(atlas.getSprite(Textures.blockRackTerminalServer));
        }
    }

    private static void renderOverlay(RackMountableRenderEvent.BlockEntity e, ResourceLocation texture) {
        renderOverlay(e, texture, 0, 1);
    }

    private static void renderOverlay(RackMountableRenderEvent.BlockEntity e, ResourceLocation texture, float u0, float u1) {
        PoseStack poseStack = e.getPoseStack();
        MultiBufferSource bufferSource = e.getBufferSource();
        if (poseStack == null || bufferSource == null) return;
        int packedOverlay = e.getPackedOverlay();
        var consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        var m = poseStack.last().pose();
        int fullBright = 0xF000F0;
        float v0 = e.v0;
        float v1 = e.v1;
        consumer.addVertex(m, u0, v1, 0).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(m, u1, v1, 0).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(m, u1, v0, 0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(m, u0, v0, 0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
    }
}
