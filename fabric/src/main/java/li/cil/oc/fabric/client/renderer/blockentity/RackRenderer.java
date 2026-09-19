package li.cil.oc.fabric.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.core.impl.common.blockentity.Rack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class RackRenderer implements BlockEntityRenderer<BlockEntity> {
  private static final float vOffset = 2 / 16f;
  private static final float vSize = 3 / 16f;

  @SuppressWarnings("unused")
  public RackRenderer(BlockEntityRendererProvider.Context ignoredContext) {
  }

  @Override
  public void render(@NotNull BlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    if (!(blockEntity instanceof Rack rack)) return;

    poseStack.pushPose();
    poseStack.translate(0.5, 0.5, 0.5);

    switch (rack.facing()) {
      case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
      case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
      case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
    }

    poseStack.translate(-0.5, 0.5, 0.505 - 0.5f / 16f);
    poseStack.scale(1, -1, 1);

    for (int i = 0; i < rack.getContainerSize(); i++) {
      if (!rack.getItem(i).isEmpty()) {
        poseStack.pushPose();
        float v0 = vOffset + i * vSize;
        float v1 = vOffset + (i + 1) * vSize;
        var event = new RackMountableRenderEvent.BlockEntity(rack, i, rack.lastData[i], v0, v1);
        event.setPoseStack(poseStack);
        event.setBufferSource(bufferSource);
        event.setPackedLight(packedLight);
        event.setPackedOverlay(packedOverlay);
        RackMountableRenderEvent.BlockEntity.EVENT.invoker().onRackMountableRender(event);
        poseStack.popPose();
      }
    }
    poseStack.popPose();
  }
}
