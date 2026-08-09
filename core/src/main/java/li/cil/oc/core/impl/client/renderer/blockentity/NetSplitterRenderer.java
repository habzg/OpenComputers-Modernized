package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.NetSplitter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public class NetSplitterRenderer implements BlockEntityRenderer<NetSplitter> {
    @SuppressWarnings("unused")
    public NetSplitterRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull NetSplitter splitter, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (java.util.Arrays.stream(net.minecraft.core.Direction.values()).noneMatch(splitter::isSideOpen)) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.0025f, -1.0025f, 1.0025f);
        poseStack.translate(-0.5, -0.5, -0.5);

        int fullBright = 0xF000F0;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(Textures.blockNetSplitterOn));
        var matrix = poseStack.last().pose();

        for (Direction side : Direction.values()) {
            if (!splitter.isSideOpen(side)) continue;

            switch (side) {
                case DOWN -> {
                    consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case UP -> {
                    consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case NORTH -> {
                    consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case SOUTH -> {
                    consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case WEST -> {
                    consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case EAST -> {
                    consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
            }
        }

        poseStack.popPose();
    }
}
