package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.Raid;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class RaidRenderer implements BlockEntityRenderer<Raid> {
    private static final float U1 = 2 / 16f;
    private static final float FS = 4 / 16f;

    @SuppressWarnings("unused")
    public RaidRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull Raid raid, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Direction yaw = raid.facing();
        switch (yaw) {
            case WEST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90)));
            case NORTH -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
            case EAST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
        }

        poseStack.translate(-0.5, 0.5, 0.505);
        poseStack.scale(1, -1, 1);

        int fullBright = 0xF000F0;

        for (int slot = 0; slot < raid.getContainerSize(); slot++) {
            if (!raid.presence[slot]) {
                renderSlot(poseStack, bufferSource, Textures.blockRaidFrontError, slot, fullBright, packedOverlay);
            }
        }

        var level = raid.getLevel();
        if (level != null) {
            for (int slot = 0; slot < raid.getContainerSize(); slot++) {
                if (System.currentTimeMillis() - raid.lastAccess < 400 && level.random.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getContainerSize()) {
                    renderSlot(poseStack, bufferSource, Textures.blockRaidFrontActivity, slot, fullBright, packedOverlay);
                }
            }
        }

        poseStack.popPose();
    }

    private void renderSlot(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.resources.ResourceLocation texture, int slot, int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        var matrix = poseStack.last().pose();
        float l = U1 + slot * FS;
        float h = U1 + (slot + 1) * FS;
        consumer.addVertex(matrix, l, 1, 0).setColor(255, 255, 255, 255).setUv(l, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, h, 1, 0).setColor(255, 255, 255, 255).setUv(h, 1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, h, 0, 0).setColor(255, 255, 255, 255).setUv(h, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, l, 0, 0).setColor(255, 255, 255, 255).setUv(l, 0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
    }
}
