package li.cil.oc.neoforge.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.client.renderer.tileentity.RenderUtil;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.tileentity.Case;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class CaseRenderer implements BlockEntityRenderer<BlockEntity> {

    @SuppressWarnings("unused")
    public CaseRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull BlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof Case computer)) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Direction yaw = computer.facing();
        switch (yaw) {
            case WEST -> poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(-90)));
            case NORTH -> poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(180)));
            case EAST -> poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(90)));
        }

        poseStack.translate(-0.5, 0.5, 0.505);
        poseStack.scale(1, -1, 1);

        int fullBright = 0xF000F0;
        if (computer.isRunning()) {
            renderOverlay(poseStack, bufferSource, Textures.blockCaseFrontOn, fullBright, packedOverlay);
            long now = System.currentTimeMillis();
            if (now - computer.lastFileSystemAccess < 400 && computer.getLevel() != null && computer.getLevel().random.nextDouble() > 0.1) {
                renderOverlay(poseStack, bufferSource, Textures.blockCaseFrontActivity, fullBright, packedOverlay);
            }
        } else if (computer.hasErrored() && RenderUtil.shouldShowErrorLight(computer.hashCode())) {
            renderOverlay(poseStack, bufferSource, Textures.blockCaseFrontError, fullBright, packedOverlay);
        }

        poseStack.popPose();
    }

    private void renderOverlay(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.resources.ResourceLocation texture, int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        var matrix = poseStack.last().pose();
        consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
    }
}
