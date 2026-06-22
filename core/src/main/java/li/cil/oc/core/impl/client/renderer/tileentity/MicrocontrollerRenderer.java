package li.cil.oc.core.impl.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.client.renderer.tileentity.RenderUtil;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.tileentity.Microcontroller;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class MicrocontrollerRenderer implements BlockEntityRenderer<Microcontroller> {
    @SuppressWarnings("unused")
    public MicrocontrollerRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull Microcontroller mcu, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Direction yaw = mcu.facing();
        switch (yaw) {
            case WEST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90)));
            case NORTH -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
            case EAST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
            case SOUTH -> {
            }
        }

        poseStack.translate(-0.5, 0.5, 0.505);
        poseStack.scale(1, -1, 1);

        int fullBright = 0xF000F0;

        renderOverlay(poseStack, bufferSource, Textures.blockMicrocontrollerFrontLight, fullBright, packedOverlay);

        if (mcu.isRunning()) {
            renderOverlay(poseStack, bufferSource, Textures.blockMicrocontrollerFrontOn, fullBright, packedOverlay);
        } else if (mcu.hasErrored() && RenderUtil.shouldShowErrorLight(mcu.hashCode())) {
            renderOverlay(poseStack, bufferSource, Textures.blockMicrocontrollerFrontError, fullBright, packedOverlay);
        }

        poseStack.popPose();
    }

    private void renderOverlay(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.resources.ResourceLocation texture, int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        var matrix = poseStack.last().pose();
        consumer.addVertex(matrix, 0, 1, 0.005f).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 1, 0.005f).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 0.005f).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 0.005f).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
    }
}
