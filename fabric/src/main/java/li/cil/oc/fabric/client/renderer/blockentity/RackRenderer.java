package li.cil.oc.fabric.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.api.event.RackMountableRenderEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class RackRenderer implements BlockEntityRenderer<BlockEntity> {
    private static final float vOffset = 2 / 16f;
    private static final float vSize = 3 / 16f;
    private static final float inset = 1 / 16f;
    private static final float depth = 15 / 16f;

    @SuppressWarnings("unused")
    public RackRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull BlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof Rack rack)) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        switch (rack.facing()) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }

        poseStack.translate(-0.5, 0.5, 0.505 - 0.5f / 16f);
        poseStack.scale(1, -1, 1);

        for (int i = 0; i < rack.getContainerSize(); i++) {
            if (!rack.getItem(i).isEmpty()) {
                poseStack.pushPose();
                float v0 = vOffset + i * vSize;
                float v1 = vOffset + (i + 1) * vSize;

                renderBaseTexture(rack, i, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.pushPose();
                poseStack.translate(inset, 0, 0);
                poseStack.scale(1 - 2 * inset, 1, 1);
                poseStack.translate(0, 0, 0.01);
                var event = new RackMountableRenderEvent.BlockEntity(rack, i, rack.lastData[i], v0, v1);
                event.setPoseStack(poseStack);
                event.setBufferSource(bufferSource);
                event.setPackedLight(packedLight);
                event.setPackedOverlay(packedOverlay);
                RackMountableRenderEvent.BlockEntity.EVENT.invoker().onRackMountableRender(event);
                poseStack.popPose();
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private void renderBaseTexture(Rack rack, int slot, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var stack = rack.getItem(slot);
        if (stack.isEmpty()) return;
        var itemInfo = li.cil.oc.api.Items.get(stack);
        if (itemInfo == null) return;

        var blockEvent = new RackMountableRenderEvent.Block(rack, slot, rack.lastData[slot], rack.facing(), null);
        RackMountableRenderEvent.Block.EVENT.invoker().onRackMountableRender(blockEvent);
        if (blockEvent.isCanceled()) return;

        var loc = baseTextureFor(itemInfo);
        if (loc == null) return;

        float v0 = vOffset + slot * vSize;
        float v1 = vOffset + (slot + 1) * vSize;
        float x1 = 1 - inset;
        float zBack = -depth;

        float cx = 0.5f, cy = (v0 + v1) / 2, cz = zBack / 2;
        float s = 0.997f;
        float sx0 = cx + (inset - cx) * s, sx1 = cx + (x1 - cx) * s;
        float sv0 = cy + (v0 - cy) * s, sv1 = cy + (v1 - cy) * s;
        float sz0 = cz + (0 - cz) * s, sz1 = cz + (zBack - cz) * s;

        var m = poseStack.last().pose();
        var genericLoc = Textures.blockGenericTop;

        var mountConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(loc));
        mountConsumer.addVertex(m, sx0, sv1, sz0).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        mountConsumer.addVertex(m, sx1, sv1, sz0).setColor(255, 255, 255, 255).setUv(1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        mountConsumer.addVertex(m, sx1, sv0, sz0).setColor(255, 255, 255, 255).setUv(1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        mountConsumer.addVertex(m, sx0, sv0, sz0).setColor(255, 255, 255, 255).setUv(0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);

        var genericConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(genericLoc));
        genericConsumer.addVertex(m, sx1, sv1, sz1).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        genericConsumer.addVertex(m, sx0, sv1, sz1).setColor(255, 255, 255, 255).setUv(1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        genericConsumer.addVertex(m, sx0, sv0, sz1).setColor(255, 255, 255, 255).setUv(1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        genericConsumer.addVertex(m, sx1, sv0, sz1).setColor(255, 255, 255, 255).setUv(0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);

        genericConsumer.addVertex(m, sx0, sv1, sz1).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        genericConsumer.addVertex(m, sx0, sv1, sz0).setColor(255, 255, 255, 255).setUv(1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        genericConsumer.addVertex(m, sx0, sv0, sz0).setColor(255, 255, 255, 255).setUv(1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        genericConsumer.addVertex(m, sx0, sv0, sz1).setColor(255, 255, 255, 255).setUv(0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);

        genericConsumer.addVertex(m, sx1, sv1, sz0).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        genericConsumer.addVertex(m, sx1, sv1, sz1).setColor(255, 255, 255, 255).setUv(1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        genericConsumer.addVertex(m, sx1, sv0, sz1).setColor(255, 255, 255, 255).setUv(1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        genericConsumer.addVertex(m, sx1, sv0, sz0).setColor(255, 255, 255, 255).setUv(0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);

        genericConsumer.addVertex(m, sx0, sv0, sz0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        genericConsumer.addVertex(m, sx1, sv0, sz0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        genericConsumer.addVertex(m, sx1, sv0, sz1).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        genericConsumer.addVertex(m, sx0, sv0, sz1).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);

        genericConsumer.addVertex(m, sx0, sv1, sz1).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        genericConsumer.addVertex(m, sx1, sv1, sz1).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        genericConsumer.addVertex(m, sx1, sv1, sz0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        genericConsumer.addVertex(m, sx0, sv1, sz0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
    }

    private static final li.cil.oc.api.detail.ItemInfo DISK_DRIVE_MOUNTABLE = li.cil.oc.api.Items.get(Constants.ItemName.DiskDriveMountable);
    private static final li.cil.oc.api.detail.ItemInfo[] SERVERS = new li.cil.oc.api.detail.ItemInfo[]{
            li.cil.oc.api.Items.get(Constants.ItemName.ServerTier1),
            li.cil.oc.api.Items.get(Constants.ItemName.ServerTier2),
            li.cil.oc.api.Items.get(Constants.ItemName.ServerTier3),
            li.cil.oc.api.Items.get(Constants.ItemName.ServerCreative)
    };
    private static final li.cil.oc.api.detail.ItemInfo TERMINAL_SERVER = li.cil.oc.api.Items.get(Constants.ItemName.TerminalServer);

    private net.minecraft.resources.ResourceLocation baseTextureFor(li.cil.oc.api.detail.ItemInfo itemInfo) {
        if (itemInfo == null) return null;
        if (itemInfo == DISK_DRIVE_MOUNTABLE) return Textures.blockRackDiskDrive;
        for (var server : SERVERS) {
            if (itemInfo == server) return Textures.blockRackServer;
        }
        if (itemInfo == TERMINAL_SERVER) return Textures.blockRackTerminalServer;
        return null;
    }
}
