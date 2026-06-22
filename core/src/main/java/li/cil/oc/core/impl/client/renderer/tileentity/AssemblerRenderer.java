package li.cil.oc.core.impl.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.tileentity.Assembler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class AssemblerRenderer implements BlockEntityRenderer<Assembler> {
    private static final int FULL_BRIGHT = 0xF000F0;

    @SuppressWarnings("unused")
    public AssemblerRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull Assembler assembler, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        renderTop(poseStack, bufferSource, packedOverlay);

        float indent = 6 / 16f + 0.005f;
        for (int i = 0; i < 4; i++) {
            if (assembler.isAssembling()) {
                renderSideInner(poseStack, bufferSource, packedOverlay, indent);
            }
            renderSideEdge(poseStack, bufferSource, packedOverlay);
            poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(90)));
        }

        poseStack.popPose();
    }

    private void renderTop(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var sprite = atlas.apply(Textures.blockAssemblerTopOnSprite);
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        var matrix = poseStack.last().pose();
        consumer.addVertex(matrix, -0.5f, 0.55f, 0.5f).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0.5f, 0.55f, 0.5f).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0.5f, 0.55f, -0.5f).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -0.5f, 0.55f, -0.5f).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private void renderSideInner(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay, float indent) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var sprite = atlas.apply(Textures.blockAssemblerSideAssemblingSprite);
        float uMin = sprite.getU((0.5f - indent));
        float uMax = sprite.getU((0.5f + indent));
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        var matrix = poseStack.last().pose();
        consumer.addVertex(matrix, indent, 0.5f, -indent).setColor(255, 255, 255, 255).setUv(uMin, v1).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, indent, 0.5f, indent).setColor(255, 255, 255, 255).setUv(uMax, v1).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, indent, -0.5f, indent).setColor(255, 255, 255, 255).setUv(uMax, v0).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, indent, -0.5f, -indent).setColor(255, 255, 255, 255).setUv(uMin, v0).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private void renderSideEdge(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var sprite = atlas.apply(Textures.blockAssemblerSideOnSprite);
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        var matrix = poseStack.last().pose();
        consumer.addVertex(matrix, 0.5005f, 0.5f, -0.5f).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0.5005f, 0.5f, 0.5f).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0.5005f, -0.5f, 0.5f).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0.5005f, -0.5f, -0.5f).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
