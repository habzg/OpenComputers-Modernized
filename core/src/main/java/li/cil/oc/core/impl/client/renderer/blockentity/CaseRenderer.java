package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.client.renderer.blockentity.RenderUtil;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.renderer.RenderHelper;
import li.cil.oc.core.impl.common.blockentity.Case;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
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

    if (computer.isRunning()) {
      renderOverlay(poseStack, bufferSource, Textures.blockCaseFrontOn);
      long now = System.currentTimeMillis();
      if (now - computer.lastFileSystemAccess < 400 && computer.getLevel() != null && computer.getLevel().random.nextDouble() > 0.1) {
        renderOverlay(poseStack, bufferSource, Textures.blockCaseFrontActivity);
      }
    } else if (computer.hasErrored() && RenderUtil.shouldShowErrorLight(computer.hashCode())) {
      renderOverlay(poseStack, bufferSource, Textures.blockCaseFrontError);
    }

    poseStack.popPose();
  }

  private void renderOverlay(PoseStack poseStack, MultiBufferSource bufferSource, net.minecraft.resources.ResourceLocation texture) {
    var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    var sprite = atlas.apply(texture);
    VertexConsumer consumer = bufferSource.getBuffer(RenderHelper.BLOCK_OVERLAY);
    var matrix = poseStack.last().pose();
    consumer.addVertex(matrix, 0, 1, 0).setUv(sprite.getU0(), sprite.getV1());
    consumer.addVertex(matrix, 1, 1, 0).setUv(sprite.getU1(), sprite.getV1());
    consumer.addVertex(matrix, 1, 0, 0).setUv(sprite.getU1(), sprite.getV0());
    consumer.addVertex(matrix, 0, 0, 0).setUv(sprite.getU0(), sprite.getV0());
  }
}
