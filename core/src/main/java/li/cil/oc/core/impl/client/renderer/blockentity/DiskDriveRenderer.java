package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.DiskDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class DiskDriveRenderer implements BlockEntityRenderer<BlockEntity> {
    @SuppressWarnings("unused")
    public DiskDriveRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull BlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof DiskDrive drive)) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Direction yaw = drive.facing();
        switch (yaw) {
            case WEST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90)));
            case NORTH -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
            case EAST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
        }

        ItemStack stack = drive.getItem(0);
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0, 3.5f / 16, 6 / 16f);
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-90)));
            poseStack.scale(0.5f, 0.5f, 0.5f);

            int brightness = drive.getLevel() != null ?
                    drive.getLevel().getLightEngine().getRawBrightness(drive.getBlockPos().relative(drive.facing()), 0) * 16 : 15728880;
            int light = brightness & 0xFFFF | (brightness << 16);

            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, drive.getLevel(), 0);

            poseStack.popPose();
        }

        if (System.currentTimeMillis() - drive.lastAccess < 400 &&
                drive.getLevel() != null && drive.getLevel().random.nextDouble() > 0.1) {
            poseStack.translate(-0.5, 0.5, 0.505);
            poseStack.scale(1, -1, 1);

            int fullBright = 0xF000F0;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(Textures.blockDiskDriveFrontActivity));
            var matrix = poseStack.last().pose();
            consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        }

        poseStack.popPose();
    }
}
