package li.cil.oc.core.impl.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class TransposerRenderer implements BlockEntityRenderer<BlockEntity> {

    @SuppressWarnings("unused")
    public TransposerRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull BlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof Environment env)) return;
        long lastOperation = env.getLastOperation();

        double activity = Math.max(0, 1 - (System.currentTimeMillis() - lastOperation) / 1000.0);
        if (activity <= 0) return;

        int alpha = (int) (activity * 255);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.0025f, -1.0025f, 1.0025f);
        poseStack.translate(-0.5, -0.5, -0.5);

        int fullBright = 0xF000F0;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(Textures.blockTransposerOn));
        var matrix = poseStack.last().pose();

        // DOWN (y=1)
        consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

        // UP (y=0)
        consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

        // NORTH (z=0)
        consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

        // SOUTH (z=1)
        consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

        // WEST (x=0)
        consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

        // EAST (x=1)
        consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

        poseStack.popPose();
    }
}
