package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.renderer.RenderHelper;
import li.cil.oc.core.impl.common.blockentity.NetSplitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class NetSplitterRenderer implements BlockEntityRenderer<NetSplitter> {
  @SuppressWarnings("unused")
  public NetSplitterRenderer(BlockEntityRendererProvider.Context ignoredContext) {
  }

  @Override
  public void render(@NotNull NetSplitter splitter, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    if (java.util.Arrays.stream(net.minecraft.core.Direction.values()).noneMatch(splitter::isSideOpen)) return;

    var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    var sprite = atlas.apply(Textures.blockNetSplitterOn);
    float u0 = sprite.getU0(), u1 = sprite.getU1();
    float v0 = sprite.getV0(), v1 = sprite.getV1();

    poseStack.pushPose();
    poseStack.translate(0.5, 0.5, 0.5);
    poseStack.scale(1.0025f, -1.0025f, 1.0025f);
    poseStack.translate(-0.5, -0.5, -0.5);

    VertexConsumer consumer = bufferSource.getBuffer(RenderHelper.BLOCK_OVERLAY);
    var matrix = poseStack.last().pose();

    for (Direction side : Direction.values()) {
      if (!splitter.isSideOpen(side)) continue;

      switch (side) {
        case DOWN -> {
          consumer.addVertex(matrix, 0, 1, 0).setUv(u0, v0);
          consumer.addVertex(matrix, 1, 1, 0).setUv(u1, v0);
          consumer.addVertex(matrix, 1, 1, 1).setUv(u1, v1);
          consumer.addVertex(matrix, 0, 1, 1).setUv(u0, v1);
        }
        case UP -> {
          consumer.addVertex(matrix, 0, 0, 0).setUv(u0, v1);
          consumer.addVertex(matrix, 0, 0, 1).setUv(u0, v0);
          consumer.addVertex(matrix, 1, 0, 1).setUv(u1, v0);
          consumer.addVertex(matrix, 1, 0, 0).setUv(u1, v1);
        }
        case NORTH -> {
          consumer.addVertex(matrix, 1, 1, 0).setUv(u0, v1);
          consumer.addVertex(matrix, 0, 1, 0).setUv(u1, v1);
          consumer.addVertex(matrix, 0, 0, 0).setUv(u1, v0);
          consumer.addVertex(matrix, 1, 0, 0).setUv(u0, v0);
        }
        case SOUTH -> {
          consumer.addVertex(matrix, 0, 1, 1).setUv(u0, v1);
          consumer.addVertex(matrix, 1, 1, 1).setUv(u1, v1);
          consumer.addVertex(matrix, 1, 0, 1).setUv(u1, v0);
          consumer.addVertex(matrix, 0, 0, 1).setUv(u0, v0);
        }
        case WEST -> {
          consumer.addVertex(matrix, 0, 1, 0).setUv(u0, v1);
          consumer.addVertex(matrix, 0, 1, 1).setUv(u1, v1);
          consumer.addVertex(matrix, 0, 0, 1).setUv(u1, v0);
          consumer.addVertex(matrix, 0, 0, 0).setUv(u0, v0);
        }
        case EAST -> {
          consumer.addVertex(matrix, 1, 1, 1).setUv(u0, v1);
          consumer.addVertex(matrix, 1, 1, 0).setUv(u1, v1);
          consumer.addVertex(matrix, 1, 0, 0).setUv(u1, v0);
          consumer.addVertex(matrix, 1, 0, 1).setUv(u0, v0);
        }
      }
    }

    poseStack.popPose();
  }
}
