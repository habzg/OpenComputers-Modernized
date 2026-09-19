package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.client.renderer.blockentity.RenderUtil;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.renderer.RenderHelper;
import li.cil.oc.core.impl.common.blockentity.Microcontroller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class MicrocontrollerRenderer implements BlockEntityRenderer<Microcontroller> {
  @SuppressWarnings("unused")
  public MicrocontrollerRenderer(BlockEntityRendererProvider.Context ignoredContext) {
  }

  @Override
  public void render(@NotNull Microcontroller mcu, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
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

    renderOverlay(atlas.apply(Textures.blockMicrocontrollerFrontLight), poseStack, bufferSource);

    if (mcu.isRunning()) {
      renderOverlay(atlas.apply(Textures.blockMicrocontrollerFrontOn), poseStack, bufferSource);
    } else if (mcu.hasErrored() && RenderUtil.shouldShowErrorLight(mcu.hashCode())) {
      renderOverlay(atlas.apply(Textures.blockMicrocontrollerFrontError), poseStack, bufferSource);
    }

    poseStack.popPose();
  }

  private void renderOverlay(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, PoseStack poseStack, MultiBufferSource bufferSource) {
    VertexConsumer consumer = bufferSource.getBuffer(RenderHelper.BLOCK_OVERLAY);
    var matrix = poseStack.last().pose();
    consumer.addVertex(matrix, 0, 1, 0).setUv(sprite.getU0(), sprite.getV1());
    consumer.addVertex(matrix, 1, 1, 0).setUv(sprite.getU1(), sprite.getV1());
    consumer.addVertex(matrix, 1, 0, 0).setUv(sprite.getU1(), sprite.getV0());
    consumer.addVertex(matrix, 0, 0, 0).setUv(sprite.getU0(), sprite.getV0());
  }
}
