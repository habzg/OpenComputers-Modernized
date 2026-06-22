package li.cil.oc.core.impl.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.tileentity.PowerDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class PowerDistributorRenderer implements BlockEntityRenderer<PowerDistributor> {
    @SuppressWarnings("unused")
    public PowerDistributorRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull PowerDistributor distributor, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (distributor.globalBuffer() <= 0) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.0025f, -1.0025f, 1.0025f);
        poseStack.translate(-0.5, -0.5, -0.5);

        int alpha = (int) ((distributor.globalBuffer() / distributor.globalBufferSize()) * 255);
        int fullBright = 0xF000F0;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var matrix = poseStack.last().pose();

        // Top face
        {
            var sprite = atlas.apply(Textures.blockPowerDistributorTopOnSprite);
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            float v0 = sprite.getV0(), v1 = sprite.getV1();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, alpha).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, alpha).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, alpha).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, alpha).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        }

        // Side faces
        {
            var sprite = atlas.apply(Textures.blockPowerDistributorSideOnSprite);
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            float v0 = sprite.getV0(), v1 = sprite.getV1();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));

            // NORTH (z = -0.0005)
            consumer.addVertex(matrix, 1, 1, -0.0005f).setColor(255, 255, 255, alpha).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 1, -0.0005f).setColor(255, 255, 255, alpha).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0, -0.0005f).setColor(255, 255, 255, alpha).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 0, -0.0005f).setColor(255, 255, 255, alpha).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

            // SOUTH (z = 1.0005)
            consumer.addVertex(matrix, 0, 1, 1.0005f).setColor(255, 255, 255, alpha).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 1, 1.0005f).setColor(255, 255, 255, alpha).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1, 0, 1.0005f).setColor(255, 255, 255, alpha).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0, 1.0005f).setColor(255, 255, 255, alpha).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

            // EAST (x = 1.0005)
            consumer.addVertex(matrix, 1.0005f, 1, 1).setColor(255, 255, 255, alpha).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1.0005f, 1, 0).setColor(255, 255, 255, alpha).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1.0005f, 0, 0).setColor(255, 255, 255, alpha).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 1.0005f, 0, 1).setColor(255, 255, 255, alpha).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);

            // WEST (x = -0.0005)
            consumer.addVertex(matrix, -0.0005f, 1, 0).setColor(255, 255, 255, alpha).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, -0.0005f, 1, 1).setColor(255, 255, 255, alpha).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, -0.0005f, 0, 1).setColor(255, 255, 255, alpha).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(matrix, -0.0005f, 0, 0).setColor(255, 255, 255, alpha).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
        }

        poseStack.popPose();
    }
}
