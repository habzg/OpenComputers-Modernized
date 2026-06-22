package li.cil.oc.neoforge.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.common.entity.Drone;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class DroneRenderer extends EntityRenderer<Drone> {
    public final ModelQuadcopter model = new ModelQuadcopter();

    @SuppressWarnings("unused")
    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(@NotNull Drone entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.pushPose();
        poseStack.translate(0, 2 / 16f, 0);

        model.applyHoverAndTilt(poseStack, entity, partialTick);
        model.setupAnim(entity, partialTick);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        if (entity.isRunning()) {
            int lightColor = entity.lightColor();

            VertexConsumer lightConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(model.texture));
            model.renderLights(poseStack, lightConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000 | (lightColor & 0x00FFFFFF));
        }

        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Drone entity) {
        return model.texture;
    }
}
